package hydro.models.access

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet

import app.models.modification.EntityTypes
import hydro.common.GuavaReplacement.ImmutableSetMultimap
import hydro.models.access.InMemoryEntityDatabase.EntitiesFetcher
import hydro.models.access.InMemoryEntityDatabase.Sortings
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.UpdatableEntity

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

/** In memory storage class that supports DbQuery operations and EntityModifications. */
final class InMemoryEntityDatabase(entitiesFetcher: EntitiesFetcher, sortings: Sortings = Sortings.create) {

  private val typeToCollection: InMemoryEntityDatabase.TypeToCollectionMap =
    new InMemoryEntityDatabase.TypeToCollectionMap(entitiesFetcher, sortings)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = {
    val entityType = implicitly[EntityType[E]]
    typeToCollection(entityType)
  }

  def update(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    typeToCollection(entityType).update(modification)
  }
}
object InMemoryEntityDatabase {

  trait EntitiesFetcher {
    def fetch[E <: Entity](entityType: EntityType[E]): Seq[E]
  }

  class Sortings private (typeToSorting: ImmutableSetMultimap[EntityType.any, DbQuery.Sorting[_]]) {
    def apply(entityType: EntityType.any): Set[DbQuery.Sorting[_]] = typeToSorting.get(entityType)

    def withSorting[E <: Entity: EntityType](sorting: DbQuery.Sorting[E]): Sortings = {
      new Sortings(typeToSorting.toBuilder.put(implicitly[EntityType[E]], sorting).build())
    }
  }
  object Sortings {
    def create: Sortings = new Sortings(ImmutableSetMultimap.of())
  }

  private final class EntityCollection[E <: Entity: EntityType](
      fetchEntities: () => Seq[E],
      sortings: Set[DbQuery.Sorting[E]],
  ) extends DbQueryExecutor.Sync[E] {

    private val idToEntityMap: ConcurrentMap[Long, E] = {
      val map = new ConcurrentHashMap[Long, E]
      for (entity <- fetchEntities()) {
        map.put(entity.id, entity)
      }
      map
    }
    private val sortingToEntities: Map[DbQuery.Sorting[E], ConcurrentSkipListSet[E]] = {
      for (sorting <- sortings) yield {
        val set = new ConcurrentSkipListSet(sorting.toOrdering)
        set.addAll(idToEntityMap.values())
        sorting -> set
      }
    }.toMap

    def update(modification: EntityModification): Unit = {
      modification match {
        case EntityModification.Add(entity) =>
          val previousValue = idToEntityMap.putIfAbsent(entity.id, entity.asInstanceOf[E])
          if (previousValue == null) {
            for (set <- sortingToEntities.values) {
              set.add(entity.asInstanceOf[E])
            }
          }
        case EntityModification.Update(entity) =>
          def updateInner[E2 <: E with UpdatableEntity] = {
            var previousValue: E2 = null.asInstanceOf[E2]
            val castEntity = entity.asInstanceOf[E2]
            idToEntityMap.computeIfPresent(
              entity.id, { (id, existingEntity) =>
                previousValue = existingEntity.asInstanceOf[E2]
                UpdatableEntity.merge(previousValue, castEntity)
              }
            )
            if (previousValue != null) {
              for (set <- sortingToEntities.values) {
                set.remove(previousValue)
                set.add(castEntity)
              }
            }
          }
          updateInner
        case EntityModification.Remove(entityId) =>
          val previousValue = idToEntityMap.remove(entityId)
          if (previousValue != null) {
            for (set <- sortingToEntities.values) {
              set.remove(previousValue)
            }
          }
      }
    }

    // **************** DbQueryExecutor.Sync **************** //
    override def data(dbQuery: DbQuery[E]): Seq[E] = valuesAsStream(dbQuery).toVector
    override def count(dbQuery: DbQuery[E]): Int = valuesAsStream(dbQuery).size

    private def valuesAsStream(dbQuery: DbQuery[E]): Stream[E] = {
      def applySorting(stream: Stream[E]): Stream[E] = dbQuery.sorting match {
        case Some(sorting) => stream.sorted(sorting.toOrdering)
        case None          => stream
      }
      def applyLimit(stream: Stream[E]): Stream[E] = dbQuery.limit match {
        case Some(limit) => stream.take(limit)
        case None        => stream
      }

      dbQuery.sorting match {
        case Some(sorting) if sortings contains sorting =>
          var stream = sortingToEntities(sorting).iterator().asScala.toStream
          stream = stream.filter(dbQuery.filter.apply)
          stream = applyLimit(stream)
          stream
        case Some(sortingReversed) if sortings contains sortingReversed.reversed =>
          var stream = sortingToEntities(sortingReversed.reversed).descendingIterator().asScala.toStream
          stream = stream.filter(dbQuery.filter.apply)
          stream = applyLimit(stream)
          stream
        case _ =>
          var stream = idToEntityMap.values().iterator().asScala.toStream
          stream = stream.filter(dbQuery.filter.apply)
          stream = applySorting(stream)
          stream = applyLimit(stream)
          stream
      }
    }
  }
  private object EntityCollection {
    type any = EntityCollection[_ <: Entity]
  }

  private final class TypeToCollectionMap(entitiesFetcher: EntitiesFetcher, sortings: Sortings) {
    private val typeToCollection: Map[EntityType.any, EntityCollection.any] = {
      for (entityType <- EntityTypes.all) yield {
        def internal[E <: Entity](
            implicit entityType: EntityType[E]): (EntityType.any, EntityCollection.any) = {
          entityType -> new EntityCollection[E](
            fetchEntities = () => entitiesFetcher.fetch(entityType),
            sortings = sortings(entityType).asInstanceOf[Set[DbQuery.Sorting[E]]])
        }
        internal(entityType)
      }
    }.toMap

    def apply[E <: Entity](entityType: EntityType[E]): EntityCollection[E] =
      typeToCollection(entityType).asInstanceOf[EntityCollection[E]]
  }
}

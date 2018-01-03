package models.access

import scala.collection.JavaConverters._
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.google.inject._
import common.time.Clock
import models.Entity
import models.access.InMemoryEntityDatabase.EntitiesFetcher
import models.accounting._
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{EntityModification, EntityModificationEntity, EntityType}
import models.money.ExchangeRateMeasurement
import models.slick.SlickUtils.dbApi._
import models.slick.SlickUtils.dbRun
import models.slick.{SlickEntityManager, SlickEntityTableDef}
import models.user.User

import scala.collection.immutable.Seq
import scala.collection.mutable

private[access] final class InMemoryEntityDatabase(entitiesFetcher: EntitiesFetcher) {

  private val typeToCollection: InMemoryEntityDatabase.TypeToCollectionMap =
    new InMemoryEntityDatabase.TypeToCollectionMap(entitiesFetcher)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = {
    val entityType = implicitly[EntityType[E]]
    typeToCollection(entityType)
  }

  def update(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    typeToCollection(entityType).update(modification)
  }
}
private[access] object InMemoryEntityDatabase {

  trait EntitiesFetcher {
    def fetch[E <: Entity](entityType: EntityType[E]): Seq[E]
  }

  private final class EntityCollection[E <: Entity: EntityType](fetchEntities: () => Seq[E])
      extends DbQueryExecutor.Sync[E] {

    private val idToEntityMap: ConcurrentMap[Long, E] = {
      val map = new ConcurrentHashMap[Long, E]
      for (entity <- fetchEntities()) {
        map.put(entity.id, entity)
      }
      map
    }

    def update(modification: EntityModification): Unit = {
      modification match {
        case EntityModification.Add(entity) => idToEntityMap.putIfAbsent(entity.id, entity.asInstanceOf[E])
        case EntityModification.Update(entity) => idToEntityMap.replace(entity.id, entity.asInstanceOf[E])
        case EntityModification.Remove(entityId) => idToEntityMap.remove(entityId)
      }
    }

    // **************** DbQueryExecutor.Sync **************** //
    override def data(dbQuery: DbQuery[E]): Seq[E] = valuesAsStream(dbQuery).toVector
    override def count(dbQuery: DbQuery[E]): Int = valuesAsStream(dbQuery).size

    private def valuesAsStream(dbQuery: DbQuery[E]): Stream[E] = {
      var stream = idToEntityMap.values().iterator().asScala.toStream
      stream = stream.filter(dbQuery.filter.apply)
      for (sorting <- dbQuery.sorting) {
        stream = stream.sorted(sorting.toOrdering)
      }
      for (limit <- dbQuery.limit) {
        stream = stream.take(limit)
      }
      stream
    }
  }
  private object EntityCollection {
    type any = EntityCollection[_ <: Entity]
  }

  private final class TypeToCollectionMap(entitiesFetcher: EntitiesFetcher) {
    private val typeToCollection: Map[EntityType.any, EntityCollection.any] = {
      for (entityType <- EntityType.values) yield {
        def internal[E <: Entity](
            implicit entityType: EntityType[E]): (EntityType.any, EntityCollection.any) = {
          entityType -> new EntityCollection[E](fetchEntities = () => entitiesFetcher.fetch(entityType))
        }
        internal(entityType)
      }
    }.toMap

    def apply[E <: Entity](entityType: EntityType[E]): EntityCollection[E] =
      typeToCollection(entityType).asInstanceOf[EntityCollection[E]]
  }
}

package hydro.models.access

import java.time.Duration
import java.util.concurrent.Executors

import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import app.models.access.ModelFields
import app.models.modification.EntityTypes
import app.models.slick.SlickEntityTableDefs
import app.models.user.User
import hydro.common.LoggingUtils.logExceptions
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.publisher.TriggerablePublisher
import hydro.common.time.Clock
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityModificationEntity
import hydro.models.modification.EntityType
import hydro.models.slick.SlickEntityTableDef
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.dbRun
import hydro.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import hydro.models.Entity
import hydro.models.slick.SlickEntityManager
import hydro.models.UpdatableEntity
import net.jcip.annotations.GuardedBy
import org.reactivestreams.Publisher

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.Future

abstract class JvmEntityAccessBase(implicit clock: Clock) extends EntityAccess {

  // lazy val because dropAndCreateTables() can be called before first fetch
  private lazy val inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase(
    entitiesFetcher = new InMemoryEntityDatabase.EntitiesFetcher {
      override def fetch[E <: Entity](entityType: EntityType[E]): Seq[E] =
        getManager(entityType).fetchAll().asInstanceOf[Seq[E]]
    },
    sortings = sortings
  )

  private val entityModificationPublisher_ : TriggerablePublisher[EntityModificationsWithToken] =
    new TriggerablePublisher()

  // **************** Methods to be overridden ****************//
  protected def getEntityTableDef(entityType: EntityType.any): SlickEntityTableDef[entityType.get]
  protected def sortings: InMemoryEntityDatabase.Sortings = InMemoryEntityDatabase.Sortings.create

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType]() = DbResultSet.fromExecutor(queryExecutor[E].asAsync)
  def newQuerySync[E <: Entity: EntityType](): DbResultSet.Sync[E] =
    DbResultSet.fromExecutor(queryExecutor)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = inMemoryEntityDatabase.queryExecutor

  def newSlickQuery[E <: Entity]()(
      implicit entityTableDef: SlickEntityTableDef[E]): TableQuery[entityTableDef.Table] =
    SlickEntityManager.forType[E].newQuery.asInstanceOf[TableQuery[entityTableDef.Table]]

  def entityModificationPublisher: Publisher[EntityModificationsWithToken] = entityModificationPublisher_

  // **************** Setters ****************//
  def persistEntityModifications(modifications: EntityModification*)(implicit user: User): Unit = {
    persistEntityModifications(modifications.toVector)
  }

  def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    Await.ready(persistEntityModificationsAsync(modifications), scala.concurrent.duration.Duration.Inf)
  }

  def persistEntityModificationsAsync(modifications: Seq[EntityModification])(
      implicit user: User): Future[Unit] = {
    EntityModificationAsyncProcessor.processAsync(modifications)
  }

  // ********** Management methods ********** //
  def dropAndCreateTables(): Unit = {
    for (tableDef <- SlickEntityTableDefs.all) {
      def internal[E <: Entity](tableDef: SlickEntityTableDef[E]) = {
        val entityManager = SlickEntityManager.forType[E](tableDef)
        dbRun(sqlu"""DROP TABLE IF EXISTS #${tableDef.tableName}""")
        entityManager.createTable()
      }
      internal(tableDef.asInstanceOf[SlickEntityTableDef[Entity]])
    }
  }

  def checkConsistentCaches(): Unit = {
    for (entityType <- EntityTypes.all) {
      def run[E <: Entity](entityType: EntityType[E]): Unit = {
        val allEntitiesInDb: Seq[E] = getManager(entityType).fetchAll().sortBy(_.id)
        val allEntitiesInMemory: Seq[E] =
          DbResultSet
            .fromExecutor(inMemoryEntityDatabase.queryExecutor(entityType))(entityType)
            .sort(DbQuery.Sorting.ascBy(ModelFields.id(entityType)))
            .data()
        require(
          allEntitiesInMemory.size == allEntitiesInDb.size,
          s"Mismatch between db and cache for entityType $entityType: Size mismatch: " +
            s"${allEntitiesInMemory.size} != ${allEntitiesInDb.size}"
        )
        require(
          allEntitiesInMemory == allEntitiesInDb,
          s"Mismatch between db and cache for entityType $entityType")
      }
      run(entityType)
    }
  }

  // ********** Private helper methods ********** //
  private def getManager(entityType: EntityType.any): SlickEntityManager[entityType.get] =
    SlickEntityManager.forType(getEntityTableDef(entityType))

  // ********** Private inner types ********** //
  private object EntityModificationAsyncProcessor {
    @GuardedBy("this")
    private val alreadySeenAddsAndRemoves: mutable.Set[EntityModification] = mutable.Set()

    private val singleThreadedExecutor =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

    def processAsync(modifications: Seq[EntityModification])(implicit user: User): Future[Unit] =
      this.synchronized {
        val uniqueModifications = modifications.filterNot(alreadySeenAddsAndRemoves)
        alreadySeenAddsAndRemoves ++= uniqueModifications.filter(m =>
          m.isInstanceOf[EntityModification.Add[_]] || m.isInstanceOf[EntityModification.Remove[_]])
        Future(processSync(uniqueModifications))(singleThreadedExecutor)
      }

    private def processSync(modifications: Seq[EntityModification])(implicit user: User): Unit =
      logExceptions {

        // Returns true if an existing modification makes the given one irrelevant.
        def eclipsedByExistingModification(
            modification: EntityModification,
            existingModifications: Iterable[EntityModification],
        ): Boolean = {
          val existingEntities = existingModifications.toStream
            .filter(_.entityId == modification.entityId)
            .filter(_.entityType == modification.entityType)
            .toSet
          val entityAlreadyRemoved =
            existingEntities.exists(_.isInstanceOf[EntityModification.Remove[_]])
          modification match {
            case _: EntityModification.Add[_]    => existingEntities.nonEmpty
            case _: EntityModification.Update[_] => entityAlreadyRemoved
            case _: EntityModification.Remove[_] => false // Always allow removes to fix inconsistencies
          }
        }

        val existingModifications: mutable.Set[EntityModification] =
          mutable.Set() ++
            dbRun(
              newSlickQuery[EntityModificationEntity]()
                .filter(_.entityId inSet modifications.map(_.entityId).toSet)
                .result)
              .map(_.modification)

        // Remove some time from the next update token because a slower persistEntityModifications() invocation B
        // could start earlier but end later than invocation A. If the WebSocket closes before the modifications B
        // get published, the `nextUpdateToken` value returned by A must be old enough so that modifications from
        // B all happened after it.
        val nextUpdateToken = toUpdateToken(clock.nowInstant minus Duration.ofSeconds(20))

        val modificationBundler = new ModificationBundler(
          triggerEveryNAdditions = 20,
          triggerFunction = modifications =>
            entityModificationPublisher_.trigger(EntityModificationsWithToken(modifications, nextUpdateToken))
        )

        for (modification <- modifications) {
          if (eclipsedByExistingModification(modification, existingModifications)) {
            println(s"  Note: Modification marked as duplicate: modification = $modification")
          } else {
            existingModifications += modification

            // Apply modification
            val entityType = modification.entityType
            modification match {
              case EntityModification.Add(entity) =>
                getManager(entityType).addNew(entityType.checkRightType(entity))
              case EntityModification.Update(entity) =>
                def updateInner[E <: entityType.get with UpdatableEntity] = {
                  implicit val castEntityType = entityType.asInstanceOf[EntityType[E]]
                  val maybeExistingEntity = DbResultSet
                    .fromExecutor(inMemoryEntityDatabase.queryExecutor[E])
                    .findOne(ModelFields.id[E] === entity.id)

                  maybeExistingEntity match {
                    case Some(existingEntity) =>
                      val mergedEntity = UpdatableEntity.merge(existingEntity, entity.asInstanceOf[E])
                      getManager(castEntityType).updateIfExists(mergedEntity)
                    case None => // Do nothing (don't upsert)
                  }
                }
                updateInner
              case EntityModification.Remove(entityId) =>
                getManager(entityType).removeIfExists(entityId)
            }

            // Add modification
            SlickEntityManager
              .forType[EntityModificationEntity]
              .addNew(
                EntityModificationEntity(
                  idOption = Some(EntityModification.generateRandomId()),
                  userId = user.id,
                  modification = modification,
                  instant = clock.nowInstant
                ))

            inMemoryEntityDatabase.update(modification)

            modificationBundler.addModificationAndMaybeTrigger(modification)
          }
        }

        modificationBundler.forceTrigger()
      }

    private class ModificationBundler(
        triggerEveryNAdditions: Int,
        triggerFunction: Seq[EntityModification] => Unit,
    ) {
      private val untriggeredModifications: mutable.Buffer[EntityModification] = mutable.Buffer()

      def addModificationAndMaybeTrigger(entityModification: EntityModification): Unit = {
        untriggeredModifications += entityModification
        if (untriggeredModifications.size >= triggerEveryNAdditions) {
          forceTrigger()
        }
      }

      def forceTrigger(): Unit = {
        if (untriggeredModifications.nonEmpty) {
          triggerFunction(untriggeredModifications.toVector)
          untriggeredModifications.clear()
        }
      }
    }
  }
}

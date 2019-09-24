package hydro.models.access

import app.api.ScalaJsApi.UpdateToken

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.api.ScalaJsApiClient
import app.models.modification.EntityTypes
import hydro.common.Listenable
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future

trait EntitySyncLogic {

  /** Populate an empty local database with entities. */
  def populateLocalDatabaseAndGetUpdateToken(db: LocalDatabase): Future[UpdateToken]

  /** Returns true if the given query can be executed on the given local database. */
  def canBeExecutedLocally[E <: Entity: EntityType](dbQuery: DbQuery[E], db: LocalDatabase): Future[Boolean]

  /**
    * Perform entity updates to the given local database as a result of the given modification.
    *
    * Note that this method may be called multiple times with the same modification.
    */
  def handleEntityModificationUpdate(
      entityModifications: Seq[EntityModification],
      db: LocalDatabase,
  ): Future[Unit]

}
object EntitySyncLogic {

  final class FullySynced(entityTypes: Seq[EntityType.any])(implicit apiClient: ScalaJsApiClient)
      extends EntitySyncLogic {

    override def populateLocalDatabaseAndGetUpdateToken(db: LocalDatabase): Future[UpdateToken] = async {
      val allEntitiesResponse = await(apiClient.getAllEntities(entityTypes))
      await(Future.sequence {
        for (entityType <- allEntitiesResponse.entityTypes) yield {
          def addAllToDb[E <: Entity](implicit entityType: EntityType[E]) =
            db.addAll(allEntitiesResponse.entities(entityType))
          addAllToDb(entityType)
        }
      })

      allEntitiesResponse.nextUpdateToken
    }

    override def canBeExecutedLocally[E <: Entity: EntityType](
        dbQuery: DbQuery[E],
        db: LocalDatabase,
    ): Future[Boolean] =
      Future.successful(entityTypes contains implicitly[EntityType[E]])

    override def handleEntityModificationUpdate(
        entityModifications: Seq[EntityModification],
        db: LocalDatabase,
    ): Future[Unit] = {
      db.applyModifications(entityModifications.filter(entityTypes contains _.entityType))
    }
  }
}

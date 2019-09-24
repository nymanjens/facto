package hydro.models.access

import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Proxy for the server-side database. */
trait RemoteDatabaseProxy {
  def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E]

  def pendingModifications(): Future[Seq[EntityModification]]

  def persistEntityModifications(modifications: Seq[EntityModification]): PersistEntityModificationsResponse

  /**
    * Start listening for entity modifications.
    *
    * Upon receiving any modifications, the given listener should be invoked.
    */
  def startCheckingForModifiedEntityUpdates(
      maybeNewEntityModificationsListener: Seq[EntityModification] => Future[Unit]): Unit

  def clearLocalDatabase(): Future[Unit]

  /**
    * If there is a local database, this future completes when it's finished loading. Otherwise, this future never
    * completes.
    */
  def localDatabaseReadyFuture: Future[Unit]

  case class PersistEntityModificationsResponse(
      queryReflectsModificationsFuture: Future[Unit],
      completelyDoneFuture: Future[Unit],
  )
}

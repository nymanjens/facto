package hydro.models.access

import app.models.Entity
import app.models.modification.EntityModification
import app.models.modification.EntityType

import scala.collection.immutable.Seq
import scala.concurrent.Future
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._
import app.models.access._

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

  case class PersistEntityModificationsResponse(queryReflectsModificationsFuture: Future[Unit],
                                                completelyDoneFuture: Future[Unit])
}

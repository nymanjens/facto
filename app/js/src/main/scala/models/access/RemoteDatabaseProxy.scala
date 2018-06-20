package models.access

import api.ScalaJsApi.UpdateToken
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala2js.Converters._

/** Proxy for the server-side database. */
private[access] trait RemoteDatabaseProxy {
  def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E]

  def pendingModifications(): Future[Seq[EntityModification]]

  def persistEntityModifications(modifications: Seq[EntityModification]): PersistEntityModificationsResponse

  /** TODO */
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

package app.models.access

import app.models.Entity
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.user.User

import scala.collection.immutable.Seq
import scala.concurrent.Future
import app.scala2js.Converters._

trait JsEntityAccess extends EntityAccess {

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  override def newQuerySyncForUser(): DbResultSet.Sync[User]

  /**
    * Returns the modifications that are incorporated into the data backing `newQuery()` ,but are not yet persisted
    * remotely.
    */
  def pendingModifications: PendingModifications

  // **************** Setters ****************//
  /**
    * Note: All read actions that are started after this call is started are postponed until the data backing
    * `newQuery()` has been updated.
    */
  def persistModifications(modifications: Seq[EntityModification]): Future[Unit]
  final def persistModifications(modifications: EntityModification*): Future[Unit] =
    persistModifications(modifications.toVector)

  def clearLocalDatabase(): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: JsEntityAccess.Listener): Unit
  private[access] def startCheckingForModifiedEntityUpdates(): Unit
}

object JsEntityAccess {

  trait Listener {

    /**
      * Called when a modification is persisted so that:
      * - Future calls to `newQuery()` will contain the given modifications
      * OR
      * - Future calls to `pendingModifications()` will have or no longer have the given modifications
      */
    def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit

    /** Called when `pendingModifications.persistedLocally` becomes true. */
    def pendingModificationsPersistedLocally(): Unit = {}
  }
}

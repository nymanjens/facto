package hydro.models.access

import hydro.common.Listenable
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

trait JsEntityAccess extends EntityAccess {

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  /**
    * Returns the modifications that are incorporated into the data backing `newQuery()` ,but are not yet persisted
    * remotely.
    */
  def pendingModifications: PendingModifications

  def localDatabaseHasBeenLoaded: Listenable[Boolean]

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
  def deregisterListener(listener: JsEntityAccess.Listener): Unit
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

package models.access

import models.modification.{EntityModification, EntityType}
import models.user.User
import models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala2js.Converters._

trait JsEntityAccess extends EntityAccess {

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  override def newQuerySyncForUser(): DbResultSet.Sync[User]

  /** Returns true if there are local pending `Add` modifications for the given entity. Note that only its id is used. */
  @Deprecated def hasLocalAddModifications[E <: Entity: EntityType](entity: E): Boolean

  // **************** Setters ****************//
  /**
    * Note: All read actions that are started after this call is started are postponed until after this write has
    * completed.
    */
  def persistModifications(modifications: Seq[EntityModification]): Future[Unit]
  final def persistModifications(modifications: EntityModification*): Future[Unit] =
    persistModifications(modifications.toVector)

  // **************** Other ****************//
  def registerListener(listener: JsEntityAccess.Listener): Unit
  private[access] def startSchedulingModifiedEntityUpdates(): Unit
}

object JsEntityAccess {

  trait Listener {

    /**
      * Future calls to `newQuery()` will contain the given modifications.
      *
      * Note that the modifications could originate from a local update (possibly not yet persisted on the server) or
      * a remote update.
      */
    def modificationsAdded(modifications: Seq[EntityModification]): Unit
  }
}

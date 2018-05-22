package common.testing

import models.Entity
import models.access.{DbQueryExecutor, DbResultSet, JsEntityAccess, PendingModifications}
import models.access.JsEntityAccess.Listener
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala2js.Converters._

final class FakeJsEntityAccess extends JsEntityAccess {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
  private var _pendingModifications: PendingModifications = PendingModifications(Set())
  private val localModificationIds: mutable.Buffer[Long] = mutable.Buffer()
  private val listeners: mutable.Buffer[Listener] = mutable.Buffer()

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def newQuery[E <: Entity: EntityType]() = {
    DbResultSet.fromExecutor(queryExecutor[E].asAsync)
  }
  override def newQuerySyncForUser() = {
    DbResultSet.fromExecutor(queryExecutor[User])
  }
  override def hasLocalAddModifications[E <: Entity: EntityType](entity: E) = {
    localModificationIds contains entity.id
  }
  override def pendingModifications: PendingModifications = _pendingModifications
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.modificationsAddedOrPendingStateChanged(modifications))
    Future.successful((): Unit)
  }
  override def registerListener(listener: Listener): Unit = {
    listeners += listener
  }
  override def startSchedulingModifiedEntityUpdates(): Unit = ???

  // **************** Additional methods for tests ****************//
  def newQuerySync[E <: Entity: EntityType](): DbResultSet.Sync[E] = DbResultSet.fromExecutor(queryExecutor)

  // TODO: Add manipulation methods for _pendingModifications
  def addRemoteModifications(modifications: Seq[EntityModification]): Unit = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.modificationsAddedOrPendingStateChanged(modifications))
  }

  def addRemotelyAddedEntities[E <: Entity: EntityType](entities: E*): Unit = {
    addRemotelyAddedEntities(entities.toVector)
  }

  def addWithRandomId[E <: Entity: EntityType](entityWithoutId: E): E = {
    val entity = entityWithoutId.withId(EntityModification.generateRandomId()).asInstanceOf[E]
    addRemotelyAddedEntities(entity)
    entity
  }

  def addRemotelyAddedEntities[E <: Entity: EntityType](entities: Seq[E]): Unit = {
    addRemoteModifications(entities map (e => EntityModification.Add(e)))
  }

  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] =
    DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E])
}

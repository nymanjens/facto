package common.testing

import jsfacades.LokiJs
import models.Entity
import models.access.RemoteDatabaseProxy
import models.access.RemoteDatabaseProxy.Listener
import models.modification.EntityType
import models.modification.EntityModification

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala2js.Converters._

final class FakeRemoteDatabaseProxy extends RemoteDatabaseProxy {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
  private val localModificationIds: mutable.Buffer[Long] = mutable.Buffer()
  private val listeners: mutable.Buffer[Listener] = mutable.Buffer()

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def newQuery[E <: Entity: EntityType]() = {
    LokiJs.ResultSet.fake(modificationsBuffer.getAllEntitiesOfType[E])
  }
  override def hasLocalAddModifications[E <: Entity: EntityType](entity: E) = {
    localModificationIds contains entity.id
  }
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.addedLocally(modifications))
    listeners.foreach(_.localModificationPersistedRemotely(modifications))
    Future.successful((): Unit)
  }
  override def clearLocalDatabase(): Future[Unit] = {
    modificationsBuffer.clear()
    Future.successful((): Unit)
  }
  override def registerListener(listener: Listener): Unit = {
    listeners += listener
  }
  override def startSchedulingModifiedEntityUpdates(): Unit = ???

  // **************** Additional methods for tests ****************//
  // TODO: Add manipulation methods for localModificationIds
  def addRemoteModifications(modifications: Seq[EntityModification]): Unit = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.addedRemotely(modifications))
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
}

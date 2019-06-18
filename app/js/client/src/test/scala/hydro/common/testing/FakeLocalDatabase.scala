package hydro.common.testing

import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQueryExecutor
import hydro.models.access.LocalDatabase
import hydro.models.access.SingletonKey

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js

final class FakeLocalDatabase extends LocalDatabase {
  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
  private val _pendingModifications: mutable.Buffer[EntityModification] = mutable.Buffer()
  private val singletonMap: mutable.Map[SingletonKey[_], js.Any] = mutable.Map()

  // **************** Getters ****************//
  override def queryExecutor[E <: Entity: EntityType]() = {
    DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E]).asAsync
  }
  override def pendingModifications() = Future.successful(_pendingModifications.toVector)
  override def getSingletonValue[V](key: SingletonKey[V]) = {
    Future.successful(singletonMap.get(key) map key.valueConverter.toScala)
  }
  override def isEmpty = {
    Future.successful(modificationsBuffer.isEmpty && singletonMap.isEmpty)
  }

  // **************** Setters ****************//
  override def applyModifications(modifications: Seq[EntityModification]) = {
    modificationsBuffer.addModifications(modifications)
    Future.successful((): Unit)
  }
  override def addAll[E <: Entity: EntityType](entities: Seq[E]) = {
    modificationsBuffer.addEntities(entities)
    Future.successful((): Unit)
  }
  override def addPendingModifications(modifications: Seq[EntityModification]) = Future.successful {
    _pendingModifications ++= modifications
  }
  override def removePendingModifications(modifications: Seq[EntityModification]) = Future.successful {
    _pendingModifications --= modifications
  }
  override def setSingletonValue[V](key: SingletonKey[V], value: V) = {
    singletonMap.put(key, key.valueConverter.toJs(value))
    Future.successful((): Unit)
  }
  override def save() = Future.successful((): Unit)
  override def resetAndInitialize() = {
    modificationsBuffer.clear()
    singletonMap.clear()
    Future.successful((): Unit)
  }

  // **************** Additional methods for tests ****************//
  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()
}

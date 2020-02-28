package hydro.models.access

import hydro.common.Annotations.visibleForTesting
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Client-side persistence layer. */
@visibleForTesting
trait LocalDatabase {
  // **************** Getters ****************//
  def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E]
  def pendingModifications(): Future[Seq[EntityModification]]
  def getSingletonValue[V](key: SingletonKey[V]): Future[Option[V]]
  def isEmpty: Future[Boolean]

  // **************** Setters ****************//
  /**
    * Applies given modification in memory but doesn't persist it in the browser's storage (call `save()` to do this).
    *
    * @return true if the in memory database changed as a result of this method
    */
  def applyModifications(modifications: Seq[EntityModification]): Future[Boolean]

  /** @return true if the in memory database changed as a result of this method */
  def addAll[E <: Entity: EntityType](entities: Seq[E]): Future[Boolean]

  /** @return true if the in memory database changed as a result of this method */
  def addPendingModifications(modifications: Seq[EntityModification]): Future[Boolean]

  /** @return true if the in memory database changed as a result of this method */
  def removePendingModifications(modifications: Seq[EntityModification]): Future[Boolean]

  /**
    * Sets given singleton value in memory.
    *
    * Note that this change is not automatically persisted in the browser's storage (call `save()` to do this).
    *
    * @return true if the in memory database changed as a result of this method
    */
  def setSingletonValue[V](key: SingletonKey[V], value: V): Future[Boolean]

  /**
    * Sets the given singleton value in memory if the key was not already associated with a value. If the
    * key was already stored, this method does nothing.
    *
    * Note that this change is not automatically persisted in the browser's storage (call `save()` to do this).
    *
    * @return true if the in memory database changed as a result of this method
    */
  def addSingletonValueIfNew[V](key: SingletonKey[V], value: V): Future[Boolean]

  /** Persists all previously made changes to the browser's storage. */
  def save(): Future[Unit]

  /** Removes all data and resets its configuration. */
  def resetAndInitialize[V](alsoSetSingleton: (SingletonKey[V], V) = null): Future[Unit]
}

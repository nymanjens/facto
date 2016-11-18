package models.manager

import scala.collection.mutable

import slick.lifted.{AbstractTable, TableQuery}
import org.apache.http.annotation.GuardedBy

import common.cache.CacheRegistry
import controllers.helpers.ControllerHelperCache

/** SlickEntityManager decorator that triggers the CacheRegistry's invalidateCache hook when an entity is updated. */
private[manager] final class InvalidatingEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: SlickEntityManager[E, T])
  extends ForwardingEntityManager[E, T](delegate) {

  // ********** Implementation of SlickEntityManager interface: Mutators ********** //
  override def add(entity: E): E = {
    val savedEntity = delegate.add(entity)
    CacheRegistry.invalidateCachesWhenUpdated(savedEntity)
    savedEntity
  }

  override def update(entity: E): E = {
    CacheRegistry.invalidateCachesWhenUpdated(entity)
    delegate.update(entity)
  }

  override def delete(entity: E): Unit = {
    CacheRegistry.invalidateCachesWhenUpdated(entity)
    delegate.delete(entity)
  }
}

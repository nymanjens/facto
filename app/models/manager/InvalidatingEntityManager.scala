package models.manager

import scala.collection.mutable

import slick.lifted.{AbstractTable, TableQuery}
import org.apache.http.annotation.GuardedBy

import common.cache.CacheMaintenanceManager
import controllers.helpers.ControllerHelperCache

/** TODO. */
private[manager] final class InvalidatingEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: EntityManager[E, T])
  extends ForwardingEntityManager[E, T](delegate) {

  // ********** Implementation of EntityManager interface: Mutators ********** //
  override def add(entity: E): E = {
    val savedEntity = delegate.add(entity)
    CacheMaintenanceManager.invalidateCachesWhenUpdated(savedEntity)
    savedEntity
  }

  override def update(entity: E): E = {
    CacheMaintenanceManager.invalidateCachesWhenUpdated(entity)
    delegate.update(entity)
  }

  override def delete(entity: E): Unit = {
    CacheMaintenanceManager.invalidateCachesWhenUpdated(entity)
    delegate.delete(entity)
  }
}

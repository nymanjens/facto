package models.manager

import scala.collection.mutable

import slick.lifted.{AbstractTable, TableQuery}
import org.apache.http.annotation.GuardedBy

import controllers.helpers.HelperCache

/** TODO. */
private[manager] final class InvalidatingEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: EntityManager[E, T])
  extends ForwardingEntityManager[E, T](delegate) {

  // ********** Implementation of EntityManager interface: Mutators ********** //
  override def add(entity: E): E = {
    val savedEntity = delegate.add(entity)
    HelperCache.invalidateCache(savedEntity)
    savedEntity
  }

  override def update(entity: E): E = {
    HelperCache.invalidateCache(entity)
    delegate.update(entity)
  }

  override def delete(entity: E): Unit = {
    HelperCache.invalidateCache(entity)
    delegate.delete(entity)
  }
}

package models.manager

import models.Entity
import slick.lifted.{AbstractTable, TableQuery}

import scala.collection.immutable.Seq

/** SlickEntityManager base implementation that forwards all calls to a given delegate. */
abstract class ForwardingEntityManager[E <: Entity, T <: AbstractTable[E]](
    delegate: SlickEntityManager[E, T])
    extends SlickEntityManager[E, T] {

  // ********** Management methods ********** //
  override def createTable(): Unit = delegate.createTable
  override def tableName: String = delegate.tableName

  // ********** Mutators ********** //
  override private[models] def addIfNew(entityWithId: E) = delegate.addIfNew(entityWithId)
  override private[models] def updateIfExists(entityWithId: E) = delegate.updateIfExists(entityWithId)
  override private[models] def deleteIfExists(entityId: Long) = delegate.deleteIfExists(entityId)

  // ********** Getters ********** //
  override def fetchAll() = delegate.fetchAll()
  override def newQuery: TableQuery[T] = delegate.newQuery
}

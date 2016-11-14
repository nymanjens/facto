package models.manager

import slick.lifted.{AbstractTable, TableQuery}

/** SlickEntityManager base implementation that forwards all calls to a given delegate. */
abstract class ForwardingEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: SlickEntityManager[E, T])
  extends SlickEntityManager[E, T] {

  // ********** Management methods ********** //
  override def initialize(): Unit = delegate.initialize()
  override def createTable(): Unit = delegate.createTable
  override def tableName: String = delegate.tableName

  // ********** Mutators ********** //
  override def add(entity: E): E = delegate.add(entity)
  override def update(entity: E): E = delegate.update(entity)
  override def delete(entity: E): Unit = delegate.delete(entity)

  // ********** Getters ********** //
  override def findById(id: Long): E = delegate.findById(id)
  override def fetchAll(): List[E] = delegate.fetchAll()
  override def newQuery: TableQuery[T] = delegate.newQuery
}

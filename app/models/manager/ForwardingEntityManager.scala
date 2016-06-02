package models.manager

import scala.collection.immutable.Seq

/** EntityManager base implementation that forwards all calls to a given delegate. */
abstract class ForwardingEntityManager[E <: Identifiable[E]](delegate: EntityManager[E]) extends EntityManager[E] {

  // ********** Management methods ********** //
  override def initialize(): Unit = delegate.initialize()
  override def verifyConsistency(): Unit = delegate.verifyConsistency()
  override def createSchema: Unit = delegate.createSchema
  override def tableName: String = delegate.tableName

  // ********** Mutators ********** //
  override def add(entity: E): E = delegate.add(entity)
  override def update(entity: E): E = delegate.update(entity)
  override def delete(entity: E): Unit = delegate.delete(entity)

  // ********** Getters ********** //
  override def findById(id: Long): E = delegate.findById(id)
  override def fetchFromAll[R](calculateResult: Stream[E] => R): R = delegate.fetchFromAll(calculateResult)
  override def fetchAll(selection: Stream[E] => Stream[E]): List[E] = delegate.fetchAll(selection)
  override def count(predicate: E => Boolean): Int = delegate.count(predicate)
}

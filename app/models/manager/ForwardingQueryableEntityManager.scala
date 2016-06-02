package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

/** EntityManager base implementation that forwards all calls to a given delegate. */
abstract class ForwardingQueryableEntityManager[E <: Identifiable[E], T <: AbstractTable[E]](delegate: QueryableEntityManager[E, T])
  extends ForwardingEntityManager[E](delegate) with QueryableEntityManager[E, T] {

  override def newQuery: TableQuery[T] = delegate.newQuery
}

package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

trait QueryableEntityManager[E <: Identifiable[E], T <: AbstractTable[_]] extends EntityManager[E] {
  def newQuery: TableQuery[T]
}

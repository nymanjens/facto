package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

import models.SlickUtils.dbApi._

/** Extends EntityManager with the ability to fire Slick database queries. */
trait QueryableEntityManager[E <: Identifiable[E], T <: AbstractTable[E]] extends EntityManager[E] {
  /** Returns a new query that should be run by models.SlickUtils.dbRun. */
  def newQuery: TableQuery[T]
}

object QueryableEntityManager {
  /** Factory method for creating a database backed QueryableEntityManager */
  def backedByDatabase[E <: Identifiable[E], T <: EntityTable[E]](cons: Tag => T,
                                                                  tableName: String
                                                                 ) = {
    new InvalidatingEntityManager[E,T](
      new DatabaseBackedEntityManager[E, T](cons, tableName)
    )
  }
}

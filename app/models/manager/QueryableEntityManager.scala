package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

import models.SlickUtils.dbApi._

trait QueryableEntityManager[E <: Identifiable[E], T <: AbstractTable[_]] extends EntityManager[E] {
  def newQuery: TableQuery[T]
}

object QueryableEntityManager {
  def backedByDatabase[E <: Identifiable[E], T <: EntityTable[E]](cons: Tag => T,
                                                                  tableName: String
                                                                 ) = new DatabaseBackedEntityManager(cons, tableName)
}
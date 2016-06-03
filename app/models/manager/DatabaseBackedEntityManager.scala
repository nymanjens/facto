package models.manager

import scala.util.{Success, Failure, Try}
import scala.collection.immutable.Seq

import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun

private[manager] final class DatabaseBackedEntityManager[E <: Identifiable[E], T <: EntityTable[E]](cons: Tag => T,
                                                                                   val tableName: String)
  extends EntityManager[E, T] {

  // ********** Implementation of EntityManager interface - Management methods ********** //
  override def createTable: Unit = {
    dbRun(newQuery.schema.create)
  }

  // ********** Implementation of EntityManager interface - Mutators ********** //
  override def add(entity: E): E = {
    require(entity.idOption.isEmpty, s"This entity was already persisted with id ${entity.id}")
    val id = dbRun(newQuery.returning(newQuery.map(_.id)).+=(entity))
    entity.withId(id)
  }

  override def update(entity: E): E = {
    mustAffectOneSingleRow {
      dbRun(newQuery.filter(_.id === entity.id).update(entity))
    }
    entity
  }

  override def delete(entity: E): Unit = {
    mustAffectOneSingleRow {
      dbRun(newQuery.filter(_.id === entity.id).delete)
    }
  }

  // ********** Implementation of EntityManager interface - Getters ********** //
  override def findById(id: Long): E = {
    dbRun(newQuery.filter(_.id === id).result) match {
      case Seq(x) => x
    }
  }

  override def fetchAll(): List[E] = {
    dbRun(newQuery.result).toList
  }

  // ********** Implementation of EntityManager interface ********** //
  override def newQuery: TableQuery[T] = new TableQuery(cons)

  protected def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}

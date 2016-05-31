package models.manager

import scala.util.{Success, Failure, Try}
import scala.collection.immutable.Seq

import models.activeslick.{Identifiable, EntityTable}
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun

final class DatabaseBackedEntityManager[E <: Identifiable[E], T <: EntityTable[E]](cons: Tag => T,
                                                                                   val tableName: String)
  extends QueryableEntityManager[E, T] {

  // ********** Implementation of EntityManager interface - Management methods ********** //
  override def createSchema: Unit = {
    dbRun(newQuery.schema.create)
  }

  // ********** Implementation of EntityManager interface - Mutators ********** //
  override def add(entity: E): E = {
    require(entity.id.isEmpty, s"This entity was already persisted with id ${entity.id.get}")
    val id = dbRun(newQuery.returning(newQuery.map(_.id)).+=(entity))
    entity.withId(id)
  }

  override def update(entity: E): Unit = {
    mustAffectOneSingleRow {
      dbRun(newQuery.filter(_.id === entity.id.get).update(entity))
    }
  }

  override def delete(entity: E): Unit = {
    mustAffectOneSingleRow {
      dbRun(newQuery.filter(_.id === entity.id.get).delete)
    }
  }

  // ********** Implementation of EntityManager interface - Getters ********** //
  override def findById(id: Long): E = {
    dbRun(newQuery.filter(_.id === id).result) match {
      case Seq(x) => x
    }
  }

  override def fetchAll(selection: Stream[E] => Stream[E]): Seq[E] = {
    dbRun(newQuery.result).toVector
  }

  // ********** Implementation of QueryableEntityManager interface ********** //
  override def newQuery: TableQuery[T] = {
    return new TableQuery(cons)
  }

  protected def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}
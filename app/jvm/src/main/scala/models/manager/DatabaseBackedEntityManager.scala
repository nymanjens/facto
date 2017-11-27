package models.manager

import models.{Entity, EntityTable}
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import play.api.Logger

import scala.collection.immutable.Seq

private[manager] final class DatabaseBackedEntityManager[E <: Entity, T <: EntityTable[E]](
    cons: Tag => T,
    val tableName: String)
    extends SlickEntityManager[E, T] {

  // ********** Implementation of SlickEntityManager interface - Management methods ********** //
  override def createTable(): Unit = {
    Logger.info(s"Creating table `$tableName`:\n        " + newQuery.schema.createStatements.mkString("\n"))
    dbRun(newQuery.schema.create)
  }

  // ********** Implementation of SlickEntityManager interface - Mutators ********** //
  override def add(entity: E): E = {
    require(entity.idOption.isEmpty, s"This entity was already persisted with id ${entity.id}")
    val id = dbRun(newQuery.returning(newQuery.map(_.id)).+=(entity))
    entity.withId(id).asInstanceOf[E]
  }

  override def addWithId(entity: E): E = {
    require(entity.idOption.isDefined, s"This entity has no id ($entity)")
    val existingEntities = dbRun(newQuery.filter(_.id === entity.id).result)
    require(
      existingEntities.isEmpty,
      s"There is already an entity with given id ${entity.id}: $existingEntities")

    mustAffectOneSingleRow {
      dbRun(newQuery.forceInsert(entity))
    }
    entity
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

  override def addIfNew(entityWithId: E) = {
    require(entityWithId.idOption.isDefined, s"This entity has no id ($entityWithId)")
    val existingEntities = dbRun(newQuery.filter(_.id === entityWithId.id).result)

    if (existingEntities.isEmpty) {
      mustAffectOneSingleRow {
        dbRun(newQuery.forceInsert(entityWithId))
      }
    }
  }

  override def updateIfExists(entityWithId: E) = {
    dbRun(newQuery.filter(_.id === entityWithId.id).update(entityWithId))
  }

  override def deleteIfExists(entityId: Long) = {
    dbRun(newQuery.filter(_.id === entityId).delete)
  }

  // ********** Implementation of SlickEntityManager interface - Getters ********** //
  override def findById(id: Long): E = {
    dbRun(newQuery.filter(_.id === id).result) match {
      case Seq(x) => x
      case Seq() => throw new IllegalArgumentException(s"Could not find entry with id=$id")
    }
  }

  override def fetchAll(): List[E] = {
    dbRun(newQuery.result).toList
  }

  // ********** Implementation of SlickEntityManager interface ********** //
  override def newQuery: TableQuery[T] = new TableQuery(cons)

  protected def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}

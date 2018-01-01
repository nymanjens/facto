package models.manager

import models.{Entity, EntityTable}
import models.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import models.SlickUtils.{database, dbRun}
import play.api.Logger

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[manager] final class DatabaseBackedEntityManager[E <: Entity, T <: EntityTable[E]](
    cons: SlickTag => T,
    val tableName: String)
    extends SlickEntityManager[E, T] {

  // ********** Implementation of SlickEntityManager interface - Management methods ********** //
  override def createTable(): Unit = {
    Logger.info(s"Creating table `$tableName`:\n        " + newQuery.schema.createStatements.mkString("\n"))
    dbRun(newQuery.schema.create)
  }

  // ********** Implementation of SlickEntityManager interface - Mutators ********** //
  override private[models] def addIfNew(entityWithId: E) = {
    require(entityWithId.idOption.isDefined, s"This entity has no id ($entityWithId)")
    val existingEntities = dbRun(newQuery.filter(_.id === entityWithId.id).result)

    if (existingEntities.isEmpty) {
      mustAffectOneSingleRow {
        dbRun(newQuery.forceInsert(entityWithId))
      }
    }
  }

  override private[models] def updateIfExists(entityWithId: E) = {
    dbRun(newQuery.filter(_.id === entityWithId.id).update(entityWithId))
  }

  override private[models] def deleteIfExists(entityId: Long) = {
    dbRun(newQuery.filter(_.id === entityId).delete)
  }

  // ********** Implementation of SlickEntityManager interface - Getters ********** //
  override def fetchAll() = async {
    await(database.run(newQuery.result)).toList
  }

  // ********** Implementation of SlickEntityManager interface ********** //
  override def newQuery: TableQuery[T] = new TableQuery(cons)

  protected def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}

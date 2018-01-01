package models

import models.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import models.SlickUtils.{database, dbRun}
import play.api.Logger

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[models] final class SlickEntityManager[E <: Entity](implicit tableDef: EntityTableDef[E]) {

  // ********** Management methods ********** //
  def createTable(): Unit = {
    Logger.info(
      s"Creating table `${tableDef.tableName}`:\n        " + newQuery.schema.createStatements.mkString("\n"))
    dbRun(newQuery.schema.create)
  }

  // ********** Mutators ********** //
  private[models] def addIfNew(entityWithId: E) = {
    require(entityWithId.idOption.isDefined, s"This entity has no id ($entityWithId)")
    val existingEntities = dbRun(newQuery.filter(_.id === entityWithId.id).result)

    if (existingEntities.isEmpty) {
      mustAffectOneSingleRow {
        dbRun(newQuery.forceInsert(entityWithId))
      }
    }
  }

  private[models] def updateIfExists(entityWithId: E) = {
    dbRun(newQuery.filter(_.id === entityWithId.id).update(entityWithId))
  }

  private[models] def deleteIfExists(entityId: Long) = {
    dbRun(newQuery.filter(_.id === entityId).delete)
  }

  // ********** Getters ********** //
  def fetchAll() = async {
    await(database.run(newQuery.result)).toList
  }

  def newQuery: TableQuery[EntityTableDef.EntityTable[E]] = new TableQuery(tableDef.table)

  private def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}

package app.models.slick

import app.models.Entity
import app.models.slick.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import app.models.slick.SlickUtils.dbRun

import scala.collection.immutable.Seq

private[models] final class SlickEntityManager[E <: Entity] private (
    implicit val tableDef: SlickEntityTableDef[E]) {

  // ********** Management methods ********** //
  def createTable(): Unit = {
    //Logger.info(
    //  s"Creating table `${tableDef.tableName}`:\n        " +
    //    newQuery.schema.createStatements.mkString("\n"))
    dbRun(newQuery.schema.create)
  }

  // ********** Mutators ********** //
  private[models] def addNew(entityWithId: E): Unit = {
    require(entityWithId.idOption.isDefined, s"This entity has no id ($entityWithId)")
    mustAffectOneSingleRow {
      dbRun(newQuery.forceInsert(entityWithId))
    }
  }

  private[models] def updateIfExists(entityWithId: E): Unit = {
    require(entityWithId.idOption.isDefined, s"This entity has no id ($entityWithId)")
    dbRun(newQuery.filter(_.id === entityWithId.id).update(entityWithId))
  }

  private[models] def deleteIfExists(entityId: Long): Unit = {
    dbRun(newQuery.filter(_.id === entityId).delete)
  }

  // ********** Getters ********** //
  def fetchAll(): Seq[E] = dbRun(newQuery).toVector

  def newQuery: TableQuery[tableDef.Table] = new TableQuery(tableDef.table)

  private def mustAffectOneSingleRow(query: => Int): Unit = {
    val affectedRows = query
    require(affectedRows == 1, s"Query affected $affectedRows rows")
  }
}
private[models] object SlickEntityManager {
  def forType[E <: Entity: SlickEntityTableDef]: SlickEntityManager[E] = new SlickEntityManager[E]
}

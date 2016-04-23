package models.activeslick

import slick.driver.H2Driver.api._

/** Table extension to be used with a Model that has an Id. */
abstract class EntityTable[M <: Identifiable[M]](tag: Tag, schemaName: Option[String], tableName: String)(implicit val colType: BaseColumnType[Long])
  extends Table[M](tag, schemaName, tableName) {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  /** Constructor without schemaName */
  def this(tag: Tag, tableName: String)(implicit mapping: BaseColumnType[Long]) = this(tag, None, tableName)
}

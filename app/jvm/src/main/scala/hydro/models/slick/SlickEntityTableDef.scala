package hydro.models.slick

import hydro.models.slick.SlickUtils.dbApi.{Table => SlickTable}
import hydro.models.slick.SlickUtils.dbApi.{Tag => SlickTag}
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.Entity

trait SlickEntityTableDef[E <: Entity] {
  type Table <: SlickEntityTableDef.EntityTable[E]
  def tableName: String
  def table(tag: SlickTag): Table
}

object SlickEntityTableDef {

  /** Table extension to be used with an Entity model. */
  // Based on active-slick (https://github.com/strongtyped/active-slick)
  abstract class EntityTable[E <: Entity](tag: SlickTag, tableName: String, schemaName: Option[String] = None)(
      implicit val colType: BaseColumnType[Long])
      extends SlickTable[E](tag, schemaName, tableName) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  }
}

package hydro.models.slick

import java.time.Instant

import app.api.Picklers._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityModificationEntity
import hydro.models.slick.SlickEntityTableDef.EntityTable
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.dbApi.{Tag => SlickTag}
import hydro.models.slick.SlickUtils.instantToSqlTimestampMapper

object StandardSlickEntityTableDefs {

  implicit object EntityModificationEntityDef extends SlickEntityTableDef[EntityModificationEntity] {

    override val tableName: String = "ENTITY_MODIFICATION_ENTITY"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[EntityModificationEntity](tag, tableName) {
      def userId = column[Long]("userId")
      def entityId = column[Long]("entityId")
      def change = column[EntityModification]("modification")
      def instant = column[Instant]("date")
      // The instant field can't hold the nano precision of the `instant` field above. It thus
      // has to be persisted separately.
      def instantNanos = column[Long]("instantNanos")

      override def * = {
        def tupled(
            tuple: (Long, Long, EntityModification, Instant, Long, Option[Long])): EntityModificationEntity =
          tuple match {
            case (userId, entityId, modification, instant, instantNanos, idOption) =>
              EntityModificationEntity(
                userId = userId,
                modification = modification,
                instant = Instant.ofEpochSecond(instant.getEpochSecond, instantNanos),
                idOption = idOption
              )
          }
        def unapply(e: EntityModificationEntity)
          : Option[(Long, Long, EntityModification, Instant, Long, Option[Long])] =
          Some((e.userId, e.modification.entityId, e.modification, e.instant, e.instant.getNano, e.idOption))

        (userId, entityId, change, instant, instantNanos, id.?) <> (tupled _, unapply _)
      }
    }

    implicit val entityModificationToBytesMapper: ColumnType[EntityModification] =
      SlickUtils.bytesMapperFromPickler
  }
}

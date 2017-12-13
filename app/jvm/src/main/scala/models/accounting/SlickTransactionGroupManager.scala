package models.accounting

import common.time.LocalDateTime
import models.EntityTable
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.accounting.SlickTransactionGroupManager.{TransactionGroups, tableName}
import models.manager.{ImmutableEntityManager, SlickEntityManager}

final class SlickTransactionGroupManager
    extends ImmutableEntityManager[TransactionGroup, TransactionGroups](
      SlickEntityManager.create[TransactionGroup, TransactionGroups](
        tag => new TransactionGroups(tag),
        tableName = tableName
      ))
    with TransactionGroup.Manager

object SlickTransactionGroupManager {
  private val tableName: String = "TRANSACTION_GROUPS"

  final class TransactionGroups(tag: SlickTag) extends EntityTable[TransactionGroup](tag, tableName) {
    def createdDate = column[LocalDateTime]("createdDate")

    override def * = (createdDate, id.?) <> (TransactionGroup.tupled, TransactionGroup.unapply)
  }
}

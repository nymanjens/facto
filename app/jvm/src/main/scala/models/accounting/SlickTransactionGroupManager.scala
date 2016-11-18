package models.accounting

import scala.collection.immutable.Seq
import org.joda.time.DateTime
import common.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.dbRun
import models.SlickUtils.JodaToSqlDateMapper
import models.accounting.money.{Money, ReferenceMoney}
import models.accounting.config.Config
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}

import SlickTransactionGroupManager.{TransactionGroups, tableName}

final class SlickTransactionGroupManager extends ImmutableEntityManager[TransactionGroup, TransactionGroups](
  SlickEntityManager.create[TransactionGroup, TransactionGroups](
    tag => new TransactionGroups(tag),
    tableName = tableName
  )) with TransactionGroup.Manager

object SlickTransactionGroupManager {
  private val tableName: String = "TRANSACTION_GROUPS"

  final class TransactionGroups(tag: SlickTag) extends EntityTable[TransactionGroup](tag, tableName) {
    def createdDate = column[DateTime]("createdDate")

    override def * = (createdDate, id.?) <> (TransactionGroup.tupled, TransactionGroup.unapply)
  }
}

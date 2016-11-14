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

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: DateTime = Clock.now,
                            idOption: Option[Long] = None) extends Entity[TransactionGroup] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions: Seq[Transaction] = dbRun(Transactions.newQuery.filter(_.transactionGroupId === id)).toList

  def isZeroSum(implicit accountingConfig: Config): Boolean = transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
}

case class TransactionGroupPartial(transactions: Seq[TransactionPartial],
                                   zeroSum: Boolean = false)

class TransactionGroups(tag: SlickTag) extends EntityTable[TransactionGroup](tag, TransactionGroups.tableName) {
  def createdDate = column[DateTime]("createdDate")

  override def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

object TransactionGroups extends ImmutableEntityManager[TransactionGroup, TransactionGroups](
  SlickEntityManager.create[TransactionGroup, TransactionGroups](
    tag => new TransactionGroups(tag), tableName = "TRANSACTION_GROUPS"))

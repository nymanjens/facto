package models.accounting

import scala.collection.immutable.Seq
import java.time.LocalDateTime
import common.time.Clock
import models.accounting.money.{Money, ReferenceMoney, ExchangeRateManager}
import models.accounting.config.Config
import models.manager.{Entity, EntityManager}
import models.EntityAccess

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime,
                            idOption: Option[Long] = None) extends Entity[TransactionGroup] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions(implicit entityAccess: EntityAccess): Seq[Transaction] =
    entityAccess.transactionManager.findByGroupId(id)

  def isZeroSum(implicit exchangeRateManager: ExchangeRateManager,
                accountingConfig: Config,
                entityAccess: EntityAccess): Boolean =
    transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
}

object TransactionGroup {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[TransactionGroup]

  case class Partial(transactions: Seq[Transaction.Partial],
                     zeroSum: Boolean = false)
}

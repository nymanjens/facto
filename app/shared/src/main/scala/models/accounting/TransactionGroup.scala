package models.accounting

import api.ScalaJsApi.EntityType

import scala.collection.immutable.Seq
import common.time.LocalDateTime
import models.accounting.money.{ExchangeRateManager, Money, ReferenceMoney}
import models.accounting.config.Config
import models.manager.{Entity, EntityManager}
import models.EntityAccess

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime,
                            idOption: Option[Long] = None) extends Entity {

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

  implicit val entityType: EntityType[TransactionGroup] = EntityType.TransactionGroupType

  trait Manager extends EntityManager[TransactionGroup]

  case class Partial(transactions: Seq[Transaction.Partial],
                     zeroSum: Boolean = false)
}

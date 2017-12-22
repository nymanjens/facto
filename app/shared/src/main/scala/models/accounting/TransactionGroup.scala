package models.accounting

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import common.money.{ExchangeRateManager, ReferenceMoney}
import common.time.LocalDateTime
import models.accounting.config.Config
import models.manager.EntityManager
import models.{Entity, EntityAccess}

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  def withTransactions(implicit entityAccess: EntityAccess): Future[TransactionGroup.WithTransactions] =
    async {
      val transactions = await(entityAccess.transactionManager.findByGroupId(id))
      TransactionGroup.WithTransactions(entity = this, transactions = transactions)
    }
}

object TransactionGroup {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[TransactionGroup]

  case class WithTransactions(entity: TransactionGroup, transactions: Seq[Transaction]) {
    def isZeroSum(implicit exchangeRateManager: ExchangeRateManager, accountingConfig: Config): Boolean =
      transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
  }

  /**
    * Same as TransactionGroup, except all fields are optional, plus an additional `transactions` and `zeroSum` which
    * a `TransactionGroup` stores implicitly.
    */
  case class Partial(transactions: Seq[Transaction.Partial],
                     zeroSum: Boolean = false,
                     createdDate: Option[LocalDateTime] = None,
                     idOption: Option[Long] = None)
  object Partial {
    val withSingleEmptyTransaction: Partial = Partial(transactions = Seq(Transaction.Partial.empty))

    def from(transactionGroup: TransactionGroup.WithTransactions)(
        implicit accountingConfig: Config,
        exchangeRateManager: ExchangeRateManager): Partial =
      Partial(
        transactions = transactionGroup.transactions.map(Transaction.Partial.from),
        zeroSum = transactionGroup.isZeroSum,
        createdDate = Some(transactionGroup.entity.createdDate),
        idOption = transactionGroup.entity.idOption
      )
  }
}

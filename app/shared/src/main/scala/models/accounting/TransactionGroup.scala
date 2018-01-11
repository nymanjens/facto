package models.accounting

import common.money.{ExchangeRateManager, ReferenceMoney}
import common.time.LocalDateTime
import models.Entity
import models.access.DbQueryImplicits._
import models.access.{DbQuery, EntityAccess, ModelField}
import models.accounting.config.Config

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions(implicit entityAccess: EntityAccess): Future[Seq[Transaction]] = {
    entityAccess
      .newQuery[Transaction]()
      .filter(ModelField.Transaction.transactionGroupId === id)
      .sort(DbQuery.Sorting.Transaction.deterministicallyByCreateDate)
      .data()
  }
}

object TransactionGroup {
  def tupled = (this.apply _).tupled

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

    def from(group: TransactionGroup, transactions: Seq[Transaction])(
        implicit accountingConfig: Config,
        exchangeRateManager: ExchangeRateManager): Partial = {
      val isZeroSum = transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
      Partial(
        transactions = transactions.map(Transaction.Partial.from),
        zeroSum = isZeroSum,
        createdDate = Some(group.createdDate),
        idOption = group.idOption
      )
    }
  }
}

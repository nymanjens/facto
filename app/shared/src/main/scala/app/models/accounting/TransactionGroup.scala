package app.models.accounting

import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import hydro.common.time.LocalDateTime
import app.models.Entity
import app.models.access.DbQueryImplicits._


import app.models.access.DbQuery
import app.models.access.AppDbQuerySorting
import app.models.access.AppDbQuerySorting
import app.models.access.EntityAccess
import app.models.access.ModelFields
import app.models.access.ModelField
import app.models.accounting.config.Config

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions(implicit entityAccess: EntityAccess): Future[Seq[Transaction]] = {
    entityAccess
      .newQuery[Transaction]()
      .filter(ModelFields.Transaction.transactionGroupId === id)
      .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
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

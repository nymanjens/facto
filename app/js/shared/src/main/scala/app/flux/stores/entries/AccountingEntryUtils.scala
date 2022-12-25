package app.flux.stores.entries

import app.common.money.ExchangeRateManager
import app.common.money.MoneyWithGeneralCurrency
import app.common.time.DatedMonth
import app.flux.stores.entries.AccountingEntryUtils.TransactionsAndBalanceChecks
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.access.ModelFields.BalanceCheck.E
import app.models.accounting.Transaction
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.BalanceCheck
import hydro.common.time.JavaTimeImplicits._
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQueryImplicits._
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.DbQuery.PicklableOrdering
import hydro.models.access.ModelField

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AccountingEntryUtils(implicit
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
) {

  def getTransactionsAndBalanceChecks(
      moneyReservoir: MoneyReservoir,
      yearFilter: Option[Int] ,
  ): Future[TransactionsAndBalanceChecks] = async {
    val upperBoundDateTime = yearFilter.map(y => DatedMonth.allMonthsIn(y).last.startTimeOfNextMonth)
    ???
  }

  def getTransactionsAndBalanceChecks(
      moneyReservoir: MoneyReservoir,
      oldestRelevantBalanceCheck: Option[BalanceCheck],
      upperBoundDateTime: Option[LocalDateTime] = None,
  ): Future[TransactionsAndBalanceChecks] = async {
    val transactionsFuture: Future[Seq[Transaction]] =
      entityAccess
        .newQuery[Transaction]()
        .filter(ModelFields.Transaction.moneyReservoirCode === moneyReservoir.code)
        .filter(
          filterBetween(
            ModelFields.Transaction.transactionDate,
            lowerBoundInclusive = oldestRelevantBalanceCheck.map(_.checkDate),
            upperBound = upperBoundDateTime,
          )
        )
        .data()

    val balanceChecksFuture: Future[Seq[BalanceCheck]] =
      entityAccess
        .newQuery[BalanceCheck]()
        .filter(ModelFields.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
        .filter(
          filterBetween(
            ModelFields.BalanceCheck.checkDate,
            lowerBoundInclusive = oldestRelevantBalanceCheck.map(_.checkDate),
            upperBound = upperBoundDateTime,
          )
        )
        .data()

    TransactionsAndBalanceChecks(
      transactions = await(transactionsFuture),
      balanceChecks = await(balanceChecksFuture),
      initialBalance = oldestRelevantBalanceCheck
        .map(_.balance)
        .getOrElse(MoneyWithGeneralCurrency(0, moneyReservoir.currency)),
      oldestRelevantBalanceCheck = oldestRelevantBalanceCheck,
    )
  }

  private def filterBetween[V: PicklableOrdering, E](
      field: ModelField[V, E],
      lowerBoundInclusive: Option[V],
      upperBound: Option[V],
  ): DbQuery.Filter[E] = {
    (lowerBoundInclusive, upperBound) match {
      case (None, None)               => DbQuery.Filter.NullFilter()
      case (Some(lower), None)        => field >= lower
      case (None, Some(upper))        => field < upper
      case (Some(lower), Some(upper)) => (field >= lower) && (field < upper)
    }
  }
}
object AccountingEntryUtils {
  case class TransactionsAndBalanceChecks(
      transactions: Seq[Transaction],
      balanceChecks: Seq[BalanceCheck],
      initialBalance: MoneyWithGeneralCurrency,
      oldestRelevantBalanceCheck: Option[BalanceCheck],
  ) {
    lazy val mergedRows: Seq[Entity] = {
      (transactions ++ balanceChecks).sortBy {
        case trans: Transaction => (trans.transactionDate, trans.createdDate)
        case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
      }
    }

    def impactingTransactionIds: Set[Long] = {
      transactions.toStream.map(_.id).toSet
    }

    def impactingBalanceCheckIds: Set[Long] = {
      (balanceChecks.toStream ++ oldestRelevantBalanceCheck).map(_.id).toSet
    }
  }
}

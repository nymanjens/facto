package app.flux.stores.entries

import app.common.accounting.DateToBalanceFunction
import app.common.money.CurrencyValueManager
import app.common.money.MoneyWithGeneralCurrency
import app.common.money.ReferenceMoney
import app.common.time.AccountingYear
import app.common.time.DatedMonth
import app.flux.stores.entries.AccountingEntryUtils.TransactionsAndBalanceChecks
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.access.ModelFields.BalanceCheck.E
import app.models.accounting.Transaction
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.BalanceCheck
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQueryImplicits._
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.DbQuery.PicklableOrdering
import hydro.models.access.ModelField

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AccountingEntryUtils(implicit
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    currencyValueManager: CurrencyValueManager,
) {

  def getTransactionsAndBalanceChecks(
      reservoir: MoneyReservoir,
      yearFilter: Option[AccountingYear],
  ): Future[TransactionsAndBalanceChecks] = async {
    val oldestRelevantBalanceCheck: Option[BalanceCheck] = yearFilter match {
      case None => None
      case Some(year) =>
        await(
          entityAccess
            .newQuery[BalanceCheck]()
            .filter(ModelFields.BalanceCheck.moneyReservoirCode === reservoir.code)
            .filter(ModelFields.BalanceCheck.checkDate < DatedMonth.allMonthsIn(year).head.startTime)
            .sort(AppDbQuerySorting.BalanceCheck.deterministicallyByCheckDate.reversed)
            .limit(1)
            .data()
        ).headOption
    }

    await(
      getTransactionsAndBalanceChecks(
        reservoir = reservoir,
        oldestRelevantBalanceCheck = oldestRelevantBalanceCheck,
        upperBoundDateTime = yearFilter.map(y => DatedMonth.allMonthsIn(y).last.startTimeOfNextMonth),
      )
    )
  }

  def getTransactionsAndBalanceChecks(
      reservoir: MoneyReservoir,
      oldestRelevantBalanceCheck: Option[BalanceCheck],
      upperBoundDateTime: Option[LocalDateTime] = None,
  ): Future[TransactionsAndBalanceChecks] = async {
    val transactionsFuture: Future[Seq[Transaction]] =
      entityAccess
        .newQuery[Transaction]()
        .filter(ModelFields.Transaction.moneyReservoirCode === reservoir.code)
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
        .filter(ModelFields.BalanceCheck.moneyReservoirCode === reservoir.code)
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
        .getOrElse(MoneyWithGeneralCurrency(0, reservoir.currency)),
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

    def oldestBalanceDate: LocalDateTime = {
      oldestRelevantBalanceCheck.map(_.checkDate).getOrElse(LocalDateTime.MIN)
    }

    def monthsCoveredByEntriesUpUntilToday(implicit clock: Clock): Seq[DatedMonth] = {
      mergedRows match {
        case Seq() => Seq()
        case _ =>
          def entityToDate(entity: Entity): LocalDateTime = {
            entity match {
              case trans: Transaction => trans.transactionDate
              case bc: BalanceCheck   => bc.checkDate
            }
          }
          DatedMonth.monthsInClosedRange(
            DatedMonth.containing(entityToDate(mergedRows.head)),
            Seq(
              DatedMonth.current,
              DatedMonth.containing(entityToDate(mergedRows.last)),
            ).max,
          )
      }
    }

    def calculateGainsInMonth(
        month: DatedMonth,
        linearGainFromMoneyFunc: GainFromMoneyFunction,
    )(implicit accountingConfig: Config): ReferenceMoney = {
      val dateToBalanceFunction = getCachedDateToBalanceFunction()

      val gainFromInitialMoney = linearGainFromMoneyFunc(
        startDate = month.startTime,
        endDate = month.startTimeOfNextMonth,
        amount = dateToBalanceFunction(month.startTime),
      )
      val gainFromUpdates =
        dateToBalanceFunction
          .updatesInRange(month)
          .map { case (date, DateToBalanceFunction.Update(balance, changeComparedToLast)) =>
            linearGainFromMoneyFunc(
              startDate = date,
              endDate = month.startTimeOfNextMonth,
              amount = changeComparedToLast,
            )
          }
          .sum
      gainFromInitialMoney + gainFromUpdates
    }

    private var dateToBalanceFunctionCache: DateToBalanceFunction = null
    def getCachedDateToBalanceFunction()(implicit accountingConfig: Config): DateToBalanceFunction = {
      if (dateToBalanceFunctionCache == null) {
        dateToBalanceFunctionCache = calculateDateToBalanceFunction()
      }
      dateToBalanceFunctionCache
    }

    private def calculateDateToBalanceFunction()(implicit accountingConfig: Config): DateToBalanceFunction = {
      val builder =
        new DateToBalanceFunction.Builder(initialDate = oldestBalanceDate, initialBalance = initialBalance)
      mergedRows.foreach {
        case transaction: Transaction =>
          builder.incrementLatestBalance(transaction.transactionDate, transaction.flow)
        case balanceCheck: BalanceCheck =>
          builder.addBalanceUpdate(balanceCheck.checkDate, balanceCheck.balance)
      }
      builder.result
    }
  }

  trait GainFromMoneyFunction {
    def apply(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        amount: MoneyWithGeneralCurrency,
    ): ReferenceMoney
  }
  object GainFromMoneyFunction {

    case class GainsFromExchangeRate(correctGainsForInflation: Boolean)(implicit
        currencyValueManager: CurrencyValueManager
    ) extends GainFromMoneyFunction {
      override def apply(
          startDate: LocalDateTime,
          endDate: LocalDateTime,
          amount: MoneyWithGeneralCurrency,
      ): ReferenceMoney = {
        val valueAtStart = amount.withDate(startDate).exchangedForReferenceCurrency()
        val valueAtEnd = amount.withDate(endDate).exchangedForReferenceCurrency()
        val result = valueAtEnd - valueAtStart
        if (correctGainsForInflation) {
          result
            .withDate(endDate)
            .exchangedForReferenceCurrency(correctForInflation = true)
        } else {
          result
        }
      }
    }

    case class GainsFromInflation()(implicit
        currencyValueManager: CurrencyValueManager
    ) extends GainFromMoneyFunction {
      override def apply(
          startDate: LocalDateTime,
          endDate: LocalDateTime,
          amount: MoneyWithGeneralCurrency,
      ): ReferenceMoney = {
        val correctedValueAtStart =
          amount.withDate(startDate).exchangedForReferenceCurrency(correctForInflation = true)
        val correctedValueAtEnd = amount
          .withDate(endDate)
          .exchangedForReferenceCurrency(correctForInflation = true)

        val currencyFluctuation =
          GainsFromExchangeRate(correctGainsForInflation = true).apply(startDate, endDate, amount)

        // Subtract currency fluctuation effects, which is already handled in SummaryExchangeRateGainsStoreFactory
        (correctedValueAtEnd - correctedValueAtStart) - currencyFluctuation
      }
    }
  }
}

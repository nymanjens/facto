package flux.stores.entries

import common.time.JavaTimeImplicits._
import common.time.{DatedMonth, LocalDateTime}
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.{
  DateToBalanceFunction,
  GainsForMonth,
  GainsForYear
}
import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.config.{Account, Config, MoneyReservoir}
import models.accounting.money.{Currency, ExchangeRateManager, MoneyWithGeneralCurrency, ReferenceMoney}
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.{Seq, SortedMap}
import scala.collection.mutable
import scala2js.Converters._
import scala2js.Keys
import scala2js.Scala2Js.Key

/**
  * Store factory that calculates the monthly gains and losses made by exchange rate fluctuations in a given year.
  */
final class SummaryExchangeRateGainsStoreFactory(implicit database: RemoteDatabaseProxy,
                                                 exchangeRateManager: ExchangeRateManager,
                                                 accountingConfig: Config,
                                                 complexQueryFilter: ComplexQueryFilter)
    extends EntriesStoreFactory[GainsForYear] {

  // **************** Public API ****************//
  def get(account: Account, year: Int): Store =
    get(Input(account = account, year = year))

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = {
      GainsForYear.sum {
        for {
          reservoir <- accountingConfig.moneyReservoirs(includeHidden = true)
          if isRelevantReservoir(reservoir)
        } yield calculateGainsForYear(reservoir)
      }
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      isRelevantReservoir(transaction.moneyReservoir) && transaction.transactionDate.getYear <= input.year
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) =
      isRelevantReservoir(balanceCheck.moneyReservoir) && balanceCheck.checkDate.getYear <= input.year

    // **************** Private helper methods ****************//
    private def calculateGainsForYear(reservoir: MoneyReservoir): GainsForYear = {
      val monthsInYear = DatedMonth.allMonthsIn(input.year)

      val oldestRelevantBalanceCheck: Option[BalanceCheck] =
        database
          .newQuery[BalanceCheck]()
          .filterEqual(Keys.BalanceCheck.moneyReservoirCode, reservoir.code)
          .filter(LokiJs.Filter.lessThan(Keys.BalanceCheck.checkDate, monthsInYear.head.startTime))
          .sort(
            LokiJs.Sorting
              .descBy(Keys.BalanceCheck.checkDate)
              .thenDescBy(Keys.BalanceCheck.createdDate)
              .thenDescBy(Keys.id))
          .limit(1)
          .data()
          .headOption
      val oldestBalanceDate = oldestRelevantBalanceCheck.map(_.checkDate).getOrElse(LocalDateTime.MIN)
      val initialBalance =
        oldestRelevantBalanceCheck.map(_.balance).getOrElse(MoneyWithGeneralCurrency(0, reservoir.currency))

      val balanceChecks: Seq[BalanceCheck] =
        database
          .newQuery[BalanceCheck]()
          .filterEqual(Keys.BalanceCheck.moneyReservoirCode, reservoir.code)
          .filter(
            filterInRange(
              Keys.BalanceCheck.checkDate,
              oldestBalanceDate,
              monthsInYear.last.startTimeOfNextMonth))
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.BalanceCheck.checkDate)
              .thenAscBy(Keys.BalanceCheck.createdDate)
              .thenAscBy(Keys.id))
          .data()

      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filterEqual(Keys.Transaction.moneyReservoirCode, reservoir.code)
          .filter(
            filterInRange(
              Keys.Transaction.transactionDate,
              oldestBalanceDate,
              monthsInYear.last.startTimeOfNextMonth))
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.Transaction.transactionDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()

      val dateToBalanceFunction: DateToBalanceFunction = {
        val builder = new DateToBalanceFunction.Builder(oldestBalanceDate, initialBalance)
        mergeTransactionsAndBalanceChecks(transactions, balanceChecks).foreach {
          case transaction: Transaction =>
            builder.incrementLatestBalance(transaction.transactionDate, transaction.flow)
          case balanceCheck: BalanceCheck =>
            builder.addBalanceUpdate(balanceCheck.checkDate, balanceCheck.balance)
        }
        builder.result
      }

      GainsForYear(
        monthToGains = monthsInYear.map { month =>
          val gain: ReferenceMoney = {
            def gainFromMoney(date: LocalDateTime, amount: MoneyWithGeneralCurrency): ReferenceMoney = {
              val valueAtDate = amount.withDate(date).exchangedForReferenceCurrency
              val valueAtEnd = amount.withDate(month.startTimeOfNextMonth).exchangedForReferenceCurrency
              valueAtEnd - valueAtDate
            }

            val gainFromIntialMoney = gainFromMoney(month.startTime, dateToBalanceFunction(month.startTime))
            val gainFromUpdates =
              dateToBalanceFunction
                .updatesInRange(month)
                .map {
                  case (date, DateToBalanceFunction.Update(balance, changeComparedToLast)) =>
                    gainFromMoney(date, changeComparedToLast)
                }
                .sum
            gainFromIntialMoney + gainFromUpdates
          }
          month -> GainsForMonth.forSingle(reservoir, gain)
        }.toMap,
        impactingTransactionIds = transactions.toStream.map(_.id).toSet,
        impactingBalanceCheckIds = (balanceChecks.toStream ++ oldestRelevantBalanceCheck).map(_.id).toSet
      )
    }

    private def isRelevantReservoir(reservoir: MoneyReservoir): Boolean =
      reservoir.owner == input.account && reservoir.currency.isForeign

    private def filterInRange[E](key: Key[LocalDateTime, E],
                                 start: LocalDateTime,
                                 end: LocalDateTime): LokiJs.Filter[E] = {
      LokiJs.Filter.and(
        LokiJs.Filter.greaterThan(key, start),
        LokiJs.Filter.lessThan(key, end)
      )
    }

    private def mergeTransactionsAndBalanceChecks(transactions: Seq[Transaction],
                                                  balanceChecks: Seq[BalanceCheck]): List[AnyRef] = {
      // merge the two (recursion does not lead to growing stack because of Stream)
      def merge(nextTransactions: List[Transaction], nextBalanceChecks: List[BalanceCheck]): Stream[AnyRef] = {
        (nextTransactions, nextBalanceChecks) match {
          case (trans :: otherTrans, bc :: otherBCs) if trans.transactionDate < bc.checkDate =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, bc :: otherBCs)
              if (trans.transactionDate == bc.checkDate) && (trans.createdDate < bc.createdDate) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, Nil) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (_, bc :: otherBCs) =>
            bc #:: merge(nextTransactions, otherBCs)
          case (Nil, Nil) =>
            Stream.empty
        }
      }
      merge(transactions.toList, balanceChecks.toList).toList
    }
  }

  /* override */
  protected case class Input(account: Account, year: Int)
}

object SummaryExchangeRateGainsStoreFactory {

  private def combineMapValues[K, V](maps: Seq[Map[K, V]])(valueCombiner: Seq[V] => V): Map[K, V] = {
    val keys = maps.map(_.keySet).reduceOption(_ union _) getOrElse Set()
    keys.map(key => key -> valueCombiner(maps.filter(_ contains key).map(_.apply(key)))).toMap
  }

  case class GainsForYear(private val monthToGains: Map[DatedMonth, GainsForMonth],
                          protected override val impactingTransactionIds: Set[Long],
                          protected override val impactingBalanceCheckIds: Set[Long])
      extends EntriesStore.StateTrait {
    def gainsForMonth(month: DatedMonth): GainsForMonth = monthToGains.getOrElse(month, GainsForMonth.empty)
    def currencies: Seq[Currency] = monthToGains.values.toStream.flatMap(_.currencies).distinct.toVector
    def nonEmpty: Boolean = this != GainsForYear.empty
  }

  object GainsForYear {
    val empty: GainsForYear = GainsForYear(Map(), Set(), Set())

    def sum(gains: Seq[GainsForYear]): GainsForYear = GainsForYear(
      monthToGains = combineMapValues(gains.map(_.monthToGains))(GainsForMonth.sum),
      impactingTransactionIds = gains.map(_.impactingTransactionIds).reduceOption(_ union _) getOrElse Set(),
      impactingBalanceCheckIds =
        gains.map(_.impactingBalanceCheckIds).reduceOption(_ union _) getOrElse Set()
    )
  }

  case class GainsForMonth private (private val reservoirToGains: Map[MoneyReservoir, ReferenceMoney]) {
    reservoirToGains.values.foreach(gain => require(!gain.isZero))

    lazy val total: ReferenceMoney = reservoirToGains.values.sum
    def nonEmpty: Boolean = reservoirToGains.nonEmpty
    def currencies: Seq[Currency] = reservoirToGains.keys.toStream.map(_.currency).distinct.toVector
    def gains(currency: Currency): ReferenceMoney =
      reservoirToGains.toStream.filter(_._1.currency == currency).map(_._2).sum
  }
  object GainsForMonth {
    val empty: GainsForMonth = GainsForMonth(Map())

    def forSingle(reservoir: MoneyReservoir, gain: ReferenceMoney): GainsForMonth = {
      if (gain.isZero) {
        empty
      } else {
        GainsForMonth(Map(reservoir -> gain))
      }
    }

    def sum(gains: Seq[GainsForMonth]): GainsForMonth =
      GainsForMonth(
        reservoirToGains = combineMapValues(gains.map(_.reservoirToGains))(_.sum).filterNot(_._2.isZero))
  }

  private[SummaryExchangeRateGainsStoreFactory] final class DateToBalanceFunction(
      dateToBalanceUpdates: SortedMap[LocalDateTime, DateToBalanceFunction.Update]) {
    def apply(date: LocalDateTime): MoneyWithGeneralCurrency = {
      dateToBalanceUpdates.to(date).values.last.balance
    }

    def updatesInRange(month: DatedMonth): SortedMap[LocalDateTime, DateToBalanceFunction.Update] =
      dateToBalanceUpdates.range(month.startTime, month.startTimeOfNextMonth)
  }

  private[SummaryExchangeRateGainsStoreFactory] object DateToBalanceFunction {
    case class Update(balance: MoneyWithGeneralCurrency, changeComparedToLast: MoneyWithGeneralCurrency)

    final class Builder(initialDate: LocalDateTime, initialBalance: MoneyWithGeneralCurrency) {
      private val dateToBalanceUpdates: mutable.SortedMap[LocalDateTime, Update] =
        mutable.SortedMap(
          initialDate -> Update(balance = initialBalance, changeComparedToLast = initialBalance))

      def incrementLatestBalance(date: LocalDateTime, addition: MoneyWithGeneralCurrency): Unit = {
        val (lastDate, lastBalance) = dateToBalanceUpdates.last
        require(lastDate <= date)
        dateToBalanceUpdates.put(
          date,
          Update(balance = lastBalance.balance + addition, changeComparedToLast = addition))
      }

      def addBalanceUpdate(date: LocalDateTime, balance: MoneyWithGeneralCurrency): Unit = {
        val (lastDate, lastBalance) = dateToBalanceUpdates.last
        require(lastDate <= date)
        dateToBalanceUpdates.put(
          date,
          Update(balance = balance, changeComparedToLast = balance - lastBalance.balance))
      }

      def result: DateToBalanceFunction =
        new DateToBalanceFunction(SortedMap.apply(dateToBalanceUpdates.toSeq: _*))
    }
  }
}

package flux.stores.entries

import java.time.LocalTime

import common.time.{DatedMonth, LocalDateTime, MonthRange}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}
import models.accounting.config.{Account, Category, Config}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala2js.Converters._
import scala2js.Keys
import scala2js.Scala2Js.Key

final class SummaryForYearStoreFactory(implicit database: RemoteDatabaseProxy,
                                       accountingConfig: Config,
                                       complexQueryFilter: ComplexQueryFilter)
    extends EntriesStoreFactory[SummaryForYear] {

  // **************** Public API ****************//
  def get(account: Account, year: Int, query: String = ""): Store =
    get(Input(account = account, year = year, query = query))

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    private val combinedFilter: LokiJs.ResultSet.Filter[Transaction] = LokiJs.ResultSet.Filter
      .and(
        LokiJs.ResultSet.Filter.equal(Keys.Transaction.beneficiaryAccountCode, input.account.code),
        filterInYear(Keys.Transaction.consumedDate, input.year),
        complexQueryFilter.fromQuery(input.query)
      )

    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(combinedFilter)
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.Transaction.consumedDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()

      val resultBuilder = new SummaryForYear.Builder
      for (transaction <- transactions) {
        resultBuilder.addTransaction(transaction)
      }
      resultBuilder.result
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      LokiJs.ResultSet.fake(Seq(transaction)).filter(combinedFilter).count() > 0
    override protected def transactionRemovalImpactsState(transactionId: Long, state: SummaryForYear) =
      state.containsTransactionId(transactionId)
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) =
      false
    override protected def balanceCheckRemovalImpactsState(balanceCheckId: Long, state: State) =
      false
  }

  /* override */
  protected case class Input(account: Account, year: Int, query: String = "")

  // **************** Private helper methods ****************//
  private def filterInYear[E](key: Key[LocalDateTime, E], year: Int): LokiJs.ResultSet.Filter[E] = {
    val yearRange = MonthRange.forYear(year)
    LokiJs.ResultSet.Filter.and(
      LokiJs.ResultSet.Filter.greaterThan(key, yearRange.startTime),
      LokiJs.ResultSet.Filter.lessThan(key, yearRange.startTimeOfNextMonth)
    )
  }
}

object SummaryForYearStoreFactory {
  case class SummaryForYear(private val cells: Map[Category, Map[DatedMonth, SummaryCell]],
                            private val transactionIds: Set[Long]) {

    def cell(category: Category, month: DatedMonth): SummaryCell = cells(category)(month)

    private[SummaryForYearStoreFactory] def containsTransactionId(id: Long): Boolean =
      transactionIds contains id

    def hasEntries(category: Category): Boolean = {
      cells(category).nonEmpty
    }
  }

  object SummaryForYear {
    private[SummaryForYearStoreFactory] class Builder(implicit accountingConfig: Config) {
      private val cells: mutable.Map[Category, mutable.Map[DatedMonth, mutable.Seq[Transaction]]] =
        mutable.Map()
      private val transactionIds: mutable.Set[Long] = mutable.Set()

      def addTransaction(transaction: Transaction): Builder = {
        def putIfMissing[K, V](map: mutable.Map[K, V], key: K)(value: => V): Unit = {
          if (!(map contains key)) {
            map.put(key, value)
          }
        }

        val category = transaction.category
        val month = DatedMonth.containing(transaction.consumedDate)

        putIfMissing(cells, category)(mutable.Map())
        putIfMissing(cells(category), month)(mutable.Seq())
        cells(transaction.category)(month) :+ transaction

        transactionIds.add(transaction.id)
        this
      }

      def result: SummaryForYear = {
        SummaryForYear(
          cells = cells.toStream
            .map {
              case (category, monthToTransactions) =>
                category -> monthToTransactions.toStream
                  .map {
                    case (month, transactions) => month -> SummaryCell(transactions.toVector)
                  }
                  .toMap
                  .withDefaultValue(SummaryCell.empty)
            }
            .toMap
            .withDefaultValue(Map().withDefaultValue(SummaryCell.empty)),
          transactionIds = transactionIds.toSet
        )
      }
    }
  }

  case class SummaryCell(transactions: Seq[Transaction]) {
    @volatile private var _totalFlow: ReferenceMoney = null

    def totalFlow(implicit exchangeRateManager: ExchangeRateManager,
                  accountingConfig: Config): ReferenceMoney = {
      if (_totalFlow == null) {
        _totalFlow = transactions.map(_.flow.exchangedForReferenceCurrency).sum
      }
      _totalFlow
    }
  }
  object SummaryCell {
    val empty: SummaryCell = SummaryCell(Seq())
  }
}

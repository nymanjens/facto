package flux.stores.entries

import common.time.{DatedMonth, LocalDateTime, MonthRange}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.config.{Account, Category, Config}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
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

      new SummaryForYear(transactions)
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
  class SummaryForYear(transactions: Seq[Transaction])(implicit accountingConfig: Config) {
    private val transactionIds: Set[Long] = transactions.toStream.map(_.id).toSet

    private val cells: Map[Category, Map[DatedMonth, SummaryCell]] =
      transactions
        .groupBy(_.category)
        .mapValues(_.groupBy(t => DatedMonth.containing(t.consumedDate)).mapValues(SummaryCell.apply))

    /** All months for which there is at least one transaction. */
    val months: Set[DatedMonth] =
      transactions.toStream.map(t => DatedMonth.containing(t.consumedDate)).toSet

    /** All categories for which there is at least one transaction. */
    def categories: Set[Category] = cells.keySet

    def cell(category: Category, month: DatedMonth): SummaryCell =
      cells.get(category).flatMap(_.get(month)) getOrElse SummaryCell.empty

    private[SummaryForYearStoreFactory] def containsTransactionId(id: Long): Boolean =
      transactionIds contains id
  }

  case class SummaryCell(transactions: Seq[Transaction]) {
    private var _totalFlow: ReferenceMoney = _

    def nonEmpty: Boolean = transactions.nonEmpty

    def totalFlow(implicit exchangeRateManager: ExchangeRateManager,
                  accountingConfig: Config): ReferenceMoney = {
      if (_totalFlow eq null) {
        _totalFlow = transactions.map(_.flow.exchangedForReferenceCurrency).sum
      }
      _totalFlow
    }
  }
  object SummaryCell {
    val empty: SummaryCell = SummaryCell(Seq())
  }
}

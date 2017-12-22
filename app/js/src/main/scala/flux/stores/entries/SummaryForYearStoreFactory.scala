package flux.stores.entries

import common.money.{ExchangeRateManager, ReferenceMoney}
import common.time.{DatedMonth, LocalDateTime}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import models.access.DbQueryImplicits._
import models.access.{DbQuery, Fields, ModelField, RemoteDatabaseProxy}
import models.accounting.config.{Account, Category, Config}
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._

final class SummaryForYearStoreFactory(implicit database: RemoteDatabaseProxy,
                                       accountingConfig: Config,
                                       complexQueryFilter: ComplexQueryFilter)
    extends EntriesStoreFactory[SummaryForYear] {

  // **************** Public API ****************//
  def get(account: Account, year: Int, query: String = ""): Store =
    get(Input(account = account, year = year, query = query))

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    private val combinedFilter: DbQuery.Filter[Transaction] =
      (Fields.Transaction.beneficiaryAccountCode isEqualTo input.account.code) &&
        filterInYear(Fields.Transaction.consumedDate, input.year) &&
        complexQueryFilter.fromQuery(input.query)

    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(combinedFilter)
          .sort(
            DbQuery.Sorting
              .ascBy(Fields.Transaction.consumedDate)
              .thenAscBy(Fields.Transaction.createdDate)
              .thenAscBy(Fields.id))
          .data()

      SummaryForYear(transactions)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      combinedFilter(transaction)
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) =
      false
  }

  /* override */
  protected case class Input(account: Account, year: Int, query: String = "")

  // **************** Private helper methods ****************//
  private def filterInYear[E](field: ModelField[LocalDateTime, E], year: Int): DbQuery.Filter[E] = {
    val months = DatedMonth.allMonthsIn(year)
    field >= months.head.startTime && field < months.last.startTimeOfNextMonth
  }
}

object SummaryForYearStoreFactory {
  case class SummaryForYear(private val transactions: Seq[Transaction])(implicit accountingConfig: Config)
      extends EntriesStore.StateTrait {
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

    override protected val impactingTransactionIds = transactions.toStream.map(_.id).toSet
    override protected def impactingBalanceCheckIds = Set()
  }

  object SummaryForYear {
    def empty(implicit accountingConfig: Config): SummaryForYear = SummaryForYear(Seq())
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

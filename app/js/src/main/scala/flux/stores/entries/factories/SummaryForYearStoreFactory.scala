package flux.stores.entries.factories

import common.money.{ExchangeRateManager, ReferenceMoney}
import common.time.{DatedMonth, LocalDateTime}
import flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import flux.stores.entries.{ComplexQueryFilter, EntriesStore}
import models.access.DbQueryImplicits._
import models.access.{DbQuery, JsEntityAccess, ModelField}
import models.accounting.config.{Account, Category, Config}
import models.accounting.{BalanceCheck, Transaction}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

final class SummaryForYearStoreFactory(implicit entityAccess: JsEntityAccess,
                                       accountingConfig: Config,
                                       complexQueryFilter: ComplexQueryFilter)
    extends EntriesStoreFactory[SummaryForYear] {

  // **************** Public API ****************//
  def get(account: Account, year: Int, query: String = ""): Store =
    get(Input(account = account, year = year, query = query))

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    private val combinedFilter: DbQuery.Filter[Transaction] =
      (ModelField.Transaction.beneficiaryAccountCode === input.account.code) &&
        filterInYear(ModelField.Transaction.consumedDate, input.year) &&
        complexQueryFilter.fromQuery(input.query)

    override protected def calculateState() = async {
      val transactions: Seq[Transaction] =
        await(
          entityAccess
            .newQuery[Transaction]()
            .filter(combinedFilter)
            .sort(DbQuery.Sorting.Transaction.deterministicallyByConsumedDate)
            .data())

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

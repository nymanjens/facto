package app.flux.stores.entries.factories

import common.money.ExchangeRateManager
import common.money.ReferenceMoney
import common.time.DatedMonth
import common.time.LocalDateTime
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import app.flux.stores.entries.ComplexQueryFilter
import app.flux.stores.entries.EntriesStore
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import app.models.accounting.config.Config
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction

import scala.async.Async.async
import scala.async.Async.await
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

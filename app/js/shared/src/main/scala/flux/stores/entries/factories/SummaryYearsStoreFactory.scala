package flux.stores.entries.factories

import common.time.YearRange
import flux.stores.entries.EntriesStore
import flux.stores.entries.factories.SummaryYearsStoreFactory.State
import models.access.DbQueryImplicits._
import models.access.{DbQuery, JsEntityAccess, ModelField}
import models.accounting.config.Account
import models.accounting.{BalanceCheck, Transaction}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

/**
  * Store factory that calculates the year span of all transactions of an account.
  *
  * The calculated range is guaranteed to contain at least all years there are transactions for but may also contain
  * more (although unlikely).
  */
final class SummaryYearsStoreFactory(implicit entityAccess: JsEntityAccess)
    extends EntriesStoreFactory[State] {

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(account: Account) = new Store {
    override protected def calculateState() = async {
      val earliestFuture = getFirstAfterSorting(DbQuery.Sorting.Transaction.deterministicallyByConsumedDate)
      val latestFuture =
        getFirstAfterSorting(DbQuery.Sorting.Transaction.deterministicallyByConsumedDate.reversed)
      val (maybeEarliest, maybeLatest) = (await(earliestFuture), await(latestFuture))

      val rangeOption = for {
        earliest <- maybeEarliest
        latest <- maybeLatest
      } yield
        State(
          YearRange.closed(earliest.consumedDate.getYear, latest.consumedDate.getYear),
          impactingTransactionIds = Set(earliest.id, latest.id))

      rangeOption getOrElse State.empty
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, oldState: State) =
      transaction.beneficiaryAccountCode == account.code && !oldState.yearRange.contains(
        transaction.consumedDate.getYear)
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false

    private def getFirstAfterSorting(sorting: DbQuery.Sorting[Transaction]): Future[Option[Transaction]] =
      async {
        val data = await(
          entityAccess
            .newQuery[Transaction]()
            .filter(ModelField.Transaction.beneficiaryAccountCode === account.code)
            .sort(sorting)
            .limit(1)
            .data())
        data match {
          case Seq()  => None
          case Seq(t) => Some(t)
        }
      }
  }

  /* override */
  protected type Input = Account
}

object SummaryYearsStoreFactory {
  case class State(yearRange: YearRange, protected override val impactingTransactionIds: Set[Long])
      extends EntriesStore.StateTrait {
    protected override def impactingBalanceCheckIds = Set()
  }
  object State {
    def empty: State = State(YearRange.empty, impactingTransactionIds = Set())
  }
}

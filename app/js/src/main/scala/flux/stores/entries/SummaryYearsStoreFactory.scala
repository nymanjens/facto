package flux.stores.entries

import common.time.YearRange
import flux.stores.entries.SummaryYearsStoreFactory.State
import models.access.DbQueryImplicits._
import models.access.{DbQuery, Fields, RemoteDatabaseProxy}
import models.accounting.config.Account
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._

/**
  * Store factory that calculates the year span of all transactions of an account.
  *
  * The calculated range is guaranteed to contain at least all years there are transactions for but may also contain
  * more (although unlikely).
  */
final class SummaryYearsStoreFactory(implicit database: RemoteDatabaseProxy)
    extends EntriesStoreFactory[State] {

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(account: Account) = new Store {
    override protected def calculateState() = {
      def getFirstAfterSorting(sorting: DbQuery.Sorting[Transaction]): Option[Transaction] = {
        val data = database
          .newQuery[Transaction]()
          .filter(Fields.Transaction.beneficiaryAccountCode isEqualTo account.code)
          .sort(sorting)
          .limit(1)
          .data()
        data match {
          case Seq() => None
          case Seq(t) => Some(t)
        }
      }

      val rangeOption = for {
        earliest <- getFirstAfterSorting(DbQuery.Sorting.ascBy(Fields.Transaction.consumedDate))
        latest <- getFirstAfterSorting(DbQuery.Sorting.descBy(Fields.Transaction.consumedDate))
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

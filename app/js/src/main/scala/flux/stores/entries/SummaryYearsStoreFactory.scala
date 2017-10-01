package flux.stores.entries

import common.time.Clock
import jsfacades.LokiJs
import jsfacades.LokiJs.Sorting
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}
import models.accounting.config.Account

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

/**
  * Store factory that calculates the year span of all transactions of an account.
  *
  * The calculated range is guaranteed to contain at least all years there are transactions for but may also contain
  * more (although unlikely).
  */
final class SummaryYearsStoreFactory(implicit database: RemoteDatabaseProxy, clock: Clock)
    extends EntriesStoreFactory[YearRange] {

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(account: Account) = new Store {
    override protected def calculateState() = {
      def getFirstAfterSorting(sorting: LokiJs.Sorting[Transaction]): Option[Transaction] = {
        val data = database
          .newQuery[Transaction]()
          .filterEqual(Keys.Transaction.beneficiaryAccountCode, account.code)
          .sort(sorting)
          .limit(1)
          .data()
        data match {
          case Seq() => None
          case Seq(t) => Some(t)
        }
      }

      val rangeOption = for {
        earliest <- getFirstAfterSorting(LokiJs.Sorting.ascBy(Keys.Transaction.consumedDate))
        latest <- getFirstAfterSorting(LokiJs.Sorting.descBy(Keys.Transaction.consumedDate))
      } yield YearRange.closed(earliest.consumedDate.getYear, latest.consumedDate.getYear)

      rangeOption getOrElse YearRange.single(clock.now.getYear)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, oldYears: YearRange) =
      transaction.beneficiaryAccountCode == account.code && !oldYears.contains(
        transaction.consumedDate.getYear)
    override protected def transactionRemovalImpactsState(transactionId: Long, state: State) = {
      // This is a heuristic because showing too many years unlikely to be an issue and generally unlikely to happen
      // anyway.
      false
    }
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
    override protected def balanceCheckRemovalImpactsState(balanceCheckId: Long, state: State) = false
  }

  /* override */
  protected type Input = Account
}

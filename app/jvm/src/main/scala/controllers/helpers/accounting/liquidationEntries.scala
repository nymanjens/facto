package controllers.helpers.accounting

import com.google.inject.{Inject, Singleton}
import collection.immutable.Seq
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.accounting.{Transaction, SlickTransactionManager}
import models.accounting.config.{Account, Category, MoneyReservoir, Config}
import models.SlickUtils.dbApi._
import controllers.helpers.ControllerHelperCache
import controllers.helpers.ControllerHelperCache.CacheIdentifier
import models.accounting.money.ReferenceMoney
import models.EntityAccess

/**
  * @param debt The debt of the first account to the second (may be negative).
  */
case class LiquidationEntry(override val transactions: Seq[Transaction], debt: ReferenceMoney)
  extends GroupedTransactions(transactions)

final class LiquidationEntries @Inject()(implicit accountingConfig: Config,
                                         transactionManager: SlickTransactionManager,
                                         entityAccess: EntityAccess) {

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(accountPair: AccountPair, n: Int): Seq[LiquidationEntry] =
    ControllerHelperCache.cached(FetchLastNEntries(accountPair, n)) {
      val allTransactions: List[Transaction] =
        dbRun(transactionManager.newQuery.sortBy(r => (r.transactionDate, r.createdDate))).toList

      val relevantTransactions =
        for {
          transaction <- allTransactions
          if isRelevantForAccounts(transaction, accountPair)
        } yield transaction

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(nextTransactions: List[Transaction], currentDebt: ReferenceMoney): Stream[LiquidationEntry] =
        nextTransactions match {
          case trans :: rest =>
            val addsTo1To2Debt = trans.beneficiary == accountPair.account2
            val flow = trans.flow.exchangedForReferenceCurrency
            val newDebt = if (addsTo1To2Debt) currentDebt + flow else currentDebt - flow
            LiquidationEntry(Seq(trans), newDebt) #:: convertToEntries(rest, newDebt)
          case Nil =>
            Stream.empty
        }
      var entries = convertToEntries(relevantTransactions, ReferenceMoney(0) /* initial debt */).toList

      entries = GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
        /* combine */ (first, last) => LiquidationEntry(first.transactions ++ last.transactions, last.debt)
      }

      entries.takeRight(n)
    }

  private def isRelevantForAccounts(transaction: Transaction, accountPair: AccountPair): Boolean = {
    val moneyReservoirOwner = transaction.moneyReservoirCode match {
      case "" =>
        // Pick the first beneficiary in the group. This simulates that the zero sum transaction was physically
        // performed on an actual reservoir, which is needed for the liquidation calculator to work.
        transaction.transactionGroup.transactions.head.beneficiary
      case _ => transaction.moneyReservoir.owner
    }
    val involvedAccounts: Set[Account] = Set(transaction.beneficiary, moneyReservoirOwner)
    accountPair.toSet == involvedAccounts
  }

  private case class FetchLastNEntries(accountPair: AccountPair, n: Int) extends CacheIdentifier[Seq[LiquidationEntry]] {
    protected override def invalidateWhenUpdating = {
      case transaction: Transaction => isRelevantForAccounts(transaction, accountPair)
    }
  }
}

package controllers.helpers.accounting

import collection.immutable.Seq

import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.accounting.{Transaction, Transactions, Money}
import models.accounting.config.{Account, MoneyReservoir, Category}
import models.SlickUtils.dbApi._

/**
  * @param debt The debt of the first account to the second (may be negative).
  */
case class LiquidationEntry(override val transactions: Seq[Transaction], debt: Money)
  extends GroupedTransactions(transactions)

object LiquidationEntry {

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(accountPair: AccountPair, n: Int): Seq[LiquidationEntry] = {
    val allTransactions: List[Transaction] =
      dbRun(Transactions.all.newQuery.sortBy(r => (r.transactionDate, r.createdDate))).toList

    val relevantTransactions =
      for {
        transaction <- allTransactions
        if isRelevantForAccounts(transaction, accountPair)
      } yield transaction

    // convert to entries (recursion does not lead to growing stack because of Stream)
    def convertToEntries(nextTransactions: List[Transaction], currentDebt: Money): Stream[LiquidationEntry] =
      nextTransactions match {
        case trans :: rest =>
          val addsTo1To2Debt = trans.beneficiary == accountPair.account2
          val newDebt = if (addsTo1To2Debt) currentDebt + trans.flow else currentDebt - trans.flow
          LiquidationEntry(Seq(trans), newDebt) #:: convertToEntries(rest, newDebt)
        case Nil =>
          Stream.empty
      }
    var entries = convertToEntries(relevantTransactions, Money(0) /* initial debt */).toList

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
        transaction.transactionGroup.transactions(0).beneficiary
      case _ => transaction.moneyReservoir.owner
    }
    val involvedAccounts: Set[Account] = Set(transaction.beneficiary, moneyReservoirOwner)
    accountPair.toSet == involvedAccounts
  }
}

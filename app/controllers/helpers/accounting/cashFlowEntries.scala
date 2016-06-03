package controllers.helpers.accounting

import collection.immutable.Seq

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import models.SlickUtils.dbApi._

import models.SlickUtils.{dbRun, JodaToSqlDateMapper}
import models.manager.Identifiable
import models.accounting._
import models.accounting.config.{Account, MoneyReservoir, Category}
import controllers.helpers.HelperCache
import controllers.helpers.HelperCache.CacheIdentifier

sealed trait CashFlowEntry

case class RegularEntry(override val transactions: Seq[Transaction], balance: Money, balanceVerified: Boolean)
  extends GroupedTransactions(transactions) with CashFlowEntry

case class BalanceCorrection(balanceCheck: BalanceCheck) extends CashFlowEntry

object CashFlowEntry {

  private case class FetchLastNEntriesCacheIdentifier(moneyReservoir: MoneyReservoir, n: Int) extends CacheIdentifier {
    override def invalidateWhenUpdating = {
      case transaction: Transaction => transaction.moneyReservoirCode == moneyReservoir.code
      case bc: BalanceCheck => bc.moneyReservoirCode == moneyReservoir.code
    }
  }

  /**
    * Returns the last n CashFlowEntries for the given reservoir, ordered from old to new.
    */
  def fetchLastNEntries(moneyReservoir: MoneyReservoir, n: Int): Seq[CashFlowEntry] =
    HelperCache.cached(FetchLastNEntriesCacheIdentifier(moneyReservoir, n)) {
      val (oldestBalanceDate, initialBalance): (DateTime, Money) = {
        val numTransactionsToFetch = 3 * n
        val totalNumTransactions = Transactions.count(_.moneyReservoirCode == moneyReservoir.code)

        if (totalNumTransactions < numTransactionsToFetch) {
          (new DateTime(0), Money(0)) // get all entries

        } else {
          // get oldest oldestTransDate
          val oldestTransDate = Transactions.fetchFromAll(_
            .filter(_.moneyReservoirCode == moneyReservoir.code)
            .sortBy(r => (r.transactionDate, r.createdDate))(Ordering[(DateTime, DateTime)].reverse)
            .take(numTransactionsToFetch)
            .last
            .transactionDate)

          // get relevant balance checks
          val oldestBC = BalanceChecks.fetchFromAll(_
            .filter(_.moneyReservoirCode == moneyReservoir.code)
            .filter(_.checkDate < oldestTransDate)
            .sortBy(r => (r.checkDate, r.createdDate))(Ordering[(DateTime, DateTime)].reverse)
            .headOption)
          val oldestBalanceDate = oldestBC.map(_.checkDate).getOrElse(new DateTime(0))
          val initialBalance = oldestBC.map(_.balance).getOrElse(Money(0L))
          (oldestBalanceDate, initialBalance)
        }
      }
      val balanceChecks: List[BalanceCheck] = BalanceChecks.fetchAll(_
        .filter(_.moneyReservoirCode == moneyReservoir.code)
        .filter(_.checkDate > oldestBalanceDate)
        .sortBy(r => (r.checkDate, r.createdDate)))

      // get relevant transactions
      val transactions: List[Transaction] = Transactions.fetchAll(_
        .filter(_.moneyReservoirCode == moneyReservoir.code)
        .filter(_.transactionDate > oldestBalanceDate)
        .sortBy(r => (r.transactionDate, r.createdDate)))

      // merge the two (recursion does not lead to growing stack because of Stream)
      def merge(nextTransactions: List[Transaction], nextBalanceChecks: List[BalanceCheck]): Stream[AnyRef] = {
        (nextTransactions, nextBalanceChecks) match {
          case (trans :: otherTrans, bc :: otherBCs) if (trans.transactionDate < bc.checkDate) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, bc :: otherBCs) if ((trans.transactionDate == bc.checkDate) && (trans.createdDate < bc.createdDate)) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, Nil) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (_, bc :: otherBCs) =>
            bc #:: merge(nextTransactions, otherBCs)
          case (Nil, Nil) =>
            Stream.empty
        }
      }
      val mergedRows = merge(transactions, balanceChecks).toList

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(nextRows: List[AnyRef], currentBalance: Money): Stream[CashFlowEntry] = (nextRows: @unchecked) match {
        case (trans: Transaction) :: rest =>
          val newBalance = currentBalance + trans.flow
          RegularEntry(List(trans), newBalance, false) #:: convertToEntries(rest, newBalance)
        case (bc: BalanceCheck) :: rest =>
          BalanceCorrection(bc) #:: convertToEntries(rest, bc.balance)
        case Nil =>
          Stream.empty
      }
      var entries = convertToEntries(mergedRows, initialBalance).toList

      // combine entries of same group and merge BC's with same balance (recursion does not lead to growing stack because of Stream)
      def combineSimilar(nextEntries: List[CashFlowEntry]): Stream[CashFlowEntry] = nextEntries match {
        case (x: RegularEntry) :: (y: RegularEntry) :: rest if (x.groupId == y.groupId) =>
          combineSimilar(RegularEntry(x.transactions ++ y.transactions, y.balance, false) :: rest)
        case (x: BalanceCorrection) :: (y: BalanceCorrection) :: rest if (x.balanceCheck.balance == y.balanceCheck.balance) =>
          combineSimilar(x :: rest)
        case entry :: rest =>
          entry #:: combineSimilar(rest)
        case Nil =>
          Stream.empty
      }
      entries = combineSimilar(entries).toList

      // merge validating BalanceCorrections into RegularEntries (recursion does not lead to growing stack because of Stream)
      def mergeValidatingBCs(nextEntries: List[CashFlowEntry]): Stream[CashFlowEntry] = nextEntries match {
        case (regular: RegularEntry) :: BalanceCorrection(bc) :: rest if (regular.balance == bc.balance) =>
          mergeValidatingBCs(regular.copy(balanceVerified = true) :: rest)
        case entry :: rest =>
          entry #:: mergeValidatingBCs(rest)
        case Nil =>
          Stream.empty
      }
      entries = mergeValidatingBCs(entries).toList

      entries.takeRight(n)
    }
}

package controllers.helpers.accounting

import com.google.inject.{Inject, Singleton}
import com.google.common.hash.Hashing
import controllers.helpers.ControllerHelperCache
import controllers.helpers.ControllerHelperCache.CacheIdentifier
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.SlickUtils.dbApi._

import common.time.JavaTimeImplicits._
import models.SlickUtils.dbRun
import models._
import models.accounting._
import models.accounting.config.{MoneyReservoir, Config}
import models.accounting.money.{DatedMoney, Money, MoneyWithGeneralCurrency}
import common.time.LocalDateTime

import scala.collection.immutable.Seq

sealed trait CashFlowEntry

case class RegularEntry(override val transactions: Seq[Transaction],
                        private val nonDatedBalance: MoneyWithGeneralCurrency,
                        balanceVerified: Boolean)
  extends GroupedTransactions(transactions) with CashFlowEntry {

  def balance: DatedMoney = {
    val latestDate = transactions.map(_.transactionDate).max
    nonDatedBalance.withDate(latestDate)
  }
}

case class BalanceCorrection(balanceCheck: BalanceCheck) extends CashFlowEntry

@Singleton()
final class CashFlowEntries @Inject()(implicit accountingConfig: Config,
                                      balanceCheckManager: SlickBalanceCheckManager,
                                      transactionManager: SlickTransactionManager) {

  /**
    * Returns the last n CashFlowEntries for the given reservoir, ordered from old to new.
    */
  def fetchLastNEntries(moneyReservoir: MoneyReservoir, n: Int): Seq[CashFlowEntry] =
  ControllerHelperCache.cached(FetchLastNEntries(moneyReservoir, n)) {
    val (oldestBalanceDate, initialBalance): (LocalDateTime, MoneyWithGeneralCurrency) = {
      val numTransactionsToFetch = 3 * n
      val totalNumTransactions = dbRun(transactionManager.newQuery
        .filter(_.moneyReservoirCode === moneyReservoir.code)
        .length
        .result)

      if (totalNumTransactions < numTransactionsToFetch) {
        (LocalDateTime.MIN, MoneyWithGeneralCurrency(0, moneyReservoir.currency)) // get all entries

      } else {
        // get oldest oldestTransDate
        val oldestTransDate = dbRun(transactionManager.newQuery
          .filter(_.moneyReservoirCode === moneyReservoir.code)
          .sortBy(r => (r.transactionDate.desc, r.createdDate.desc))
          .take(numTransactionsToFetch))
          .last
          .transactionDate

        // get relevant balance checks
        val oldestBC = dbRun(balanceCheckManager.newQuery
          .filter(_.moneyReservoirCode === moneyReservoir.code)
          .filter(_.checkDate < oldestTransDate)
          .sortBy(r => (r.checkDate.desc, r.createdDate.desc))
          .take(1))
          .headOption
        val oldestBalanceDate = oldestBC.map(_.checkDate).getOrElse(LocalDateTime.MIN)
        val initialBalance = oldestBC.map(_.balance).getOrElse(MoneyWithGeneralCurrency(0, moneyReservoir.currency))
        (oldestBalanceDate, initialBalance)
      }
    }
    val balanceChecks: List[BalanceCheck] = dbRun(balanceCheckManager.newQuery
      .filter(_.moneyReservoirCode === moneyReservoir.code)
      .filter(_.checkDate > oldestBalanceDate)
      .sortBy(r => (r.checkDate, r.createdDate)))
      .toList

    // get relevant transactions
    val transactions: List[Transaction] = dbRun(transactionManager.newQuery
      .filter(_.moneyReservoirCode === moneyReservoir.code)
      .filter(_.transactionDate > oldestBalanceDate)
      .sortBy(r => (r.transactionDate, r.createdDate)))
      .toList

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
    def convertToEntries(nextRows: List[AnyRef], currentBalance: MoneyWithGeneralCurrency): Stream[CashFlowEntry] = (nextRows: @unchecked) match {
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

private case class FetchLastNEntries(moneyReservoir: MoneyReservoir, n: Int) extends CacheIdentifier[Seq[CashFlowEntry]] {
  protected override def invalidateWhenUpdating = {
    case transaction: Transaction => transaction.moneyReservoirCode == moneyReservoir.code
    case bc: BalanceCheck => bc.moneyReservoirCode == moneyReservoir.code
  }
}

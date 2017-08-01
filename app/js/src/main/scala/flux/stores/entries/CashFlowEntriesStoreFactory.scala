package flux.stores.entries

import scala2js.Converters._
import scala2js.Keys
import common.time.JavaTimeImplicits._
import common.time.LocalDateTime
import flux.stores.entries.CashFlowEntry.{BalanceCorrection, RegularEntry}
import jsfacades.Loki
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.config.{Config, MoneyReservoir}
import models.accounting.money.{ExchangeRateManager, MoneyWithGeneralCurrency}
import models.accounting.{Transaction, _}

import scala2js.Converters._

final class CashFlowEntriesStoreFactory(implicit database: RemoteDatabaseProxy,
                                        accountingConfig: Config,
                                        exchangeRateManager: ExchangeRateManager,
                                        entityAccess: EntityAccess)
    extends EntriesListStoreFactory[CashFlowEntry, MoneyReservoir] {

  override protected def createNew(maxNumEntries: Int, moneyReservoir: MoneyReservoir) = new Store {
    override protected def calculateState() = {
      val (oldestBalanceDate, initialBalance): (LocalDateTime, MoneyWithGeneralCurrency) = {
        val numTransactionsToFetch = 3 * maxNumEntries
        val totalNumTransactions =
          database
            .newQuery[Transaction]()
            .filter(Keys.Transaction.moneyReservoirCode, moneyReservoir.code)
            .count()

        if (totalNumTransactions < numTransactionsToFetch) {
          (LocalDateTime.MIN, MoneyWithGeneralCurrency(0, moneyReservoir.currency)) // get all entries

        } else {
          // get oldest oldestTransDate
          val oldestTransDate =
            database
              .newQuery[Transaction]()
              .filter(Keys.Transaction.moneyReservoirCode, moneyReservoir.code)
              .sort(
                Loki.Sorting
                  .descBy("transactionDate")
                  .thenDescBy("createdDate")
                  .thenDescBy("id"))
              .limit(numTransactionsToFetch)
              .data()
              .last
              .transactionDate

          // get relevant balance checks
          val oldestBC =
            database
              .newQuery[BalanceCheck]()
              .filter(Keys.BalanceCheck.moneyReservoirCode, moneyReservoir.code)
              .filterLessThan(Keys.BalanceCheck.checkDate, oldestTransDate)
              .sort(
                Loki.Sorting
                  .descBy("checkDate")
                  .thenDescBy("createdDate")
                  .thenDescBy("id"))
              .limit(1)
              .data()
              .headOption
          val oldestBalanceDate = oldestBC.map(_.checkDate).getOrElse(LocalDateTime.MIN)
          val initialBalance =
            oldestBC.map(_.balance).getOrElse(MoneyWithGeneralCurrency(0, moneyReservoir.currency))
          (oldestBalanceDate, initialBalance)
        }
      }

      val balanceChecks: Seq[BalanceCheck] =
        database
          .newQuery[BalanceCheck]()
          .filter(Keys.BalanceCheck.moneyReservoirCode, moneyReservoir.code)
          .filterGreaterThan( Keys.BalanceCheck.checkDate,  oldestBalanceDate)
          .sort(
            Loki.Sorting
              .ascBy("checkDate")
              .thenAscBy("createdDate")
              .thenAscBy("id"))
          .data()

      // get relevant transactions
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(Keys.Transaction.moneyReservoirCode, moneyReservoir.code)
          .filterGreaterThan(Keys.Transaction.transactionDate, oldestBalanceDate)
          .sort(
            Loki.Sorting
              .ascBy("transactionDate")
              .thenAscBy("createdDate")
              .thenAscBy("id"))
          .data()

      // merge the two (recursion does not lead to growing stack because of Stream)
      def merge(nextTransactions: List[Transaction], nextBalanceChecks: List[BalanceCheck]): Stream[AnyRef] = {
        (nextTransactions, nextBalanceChecks) match {
          case (trans :: otherTrans, bc :: otherBCs) if trans.transactionDate < bc.checkDate =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, bc :: otherBCs)
              if (trans.transactionDate == bc.checkDate) && (trans.createdDate < bc.createdDate) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (trans :: otherTrans, Nil) =>
            trans #:: merge(otherTrans, nextBalanceChecks)
          case (_, bc :: otherBCs) =>
            bc #:: merge(nextTransactions, otherBCs)
          case (Nil, Nil) =>
            Stream.empty
        }
      }
      val mergedRows = merge(transactions.toList, balanceChecks.toList).toList

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(nextRows: List[AnyRef],
                           currentBalance: MoneyWithGeneralCurrency): Stream[CashFlowEntry] =
        (nextRows: @unchecked) match {
          case (trans: Transaction) :: rest =>
            val newBalance = currentBalance + trans.flow
            RegularEntry(List(trans), newBalance, balanceVerified = false) #:: convertToEntries(
              rest,
              newBalance)
          case (bc: BalanceCheck) :: rest =>
            BalanceCorrection(bc) #:: convertToEntries(rest, bc.balance)
          case Nil =>
            Stream.empty
        }
      var entries = convertToEntries(mergedRows, initialBalance).toList

      // combine entries of same group and merge BC's with same balance (recursion does not lead to growing stack because of Stream)
      def combineSimilar(nextEntries: List[CashFlowEntry]): Stream[CashFlowEntry] = nextEntries match {
        case (x: RegularEntry) :: (y: RegularEntry) :: rest if x.groupId == y.groupId =>
          combineSimilar(
            RegularEntry(x.transactions ++ y.transactions, y.balance, balanceVerified = false) :: rest)
        case (x: BalanceCorrection) :: (y: BalanceCorrection) :: rest
            if x.balanceCheck.balance == y.balanceCheck.balance =>
          combineSimilar(x :: rest)
        case entry :: rest =>
          entry #:: combineSimilar(rest)
        case Nil =>
          Stream.empty
      }
      entries = combineSimilar(entries).toList

      // merge validating BalanceCorrections into RegularEntries (recursion does not lead to growing stack because of Stream)
      def mergeValidatingBCs(nextEntries: List[CashFlowEntry]): Stream[CashFlowEntry] = nextEntries match {
        case (regular: RegularEntry) :: BalanceCorrection(bc) :: rest if regular.balance == bc.balance =>
          mergeValidatingBCs(regular.copy(balanceVerified = true) :: rest)
        case entry :: rest =>
          entry #:: mergeValidatingBCs(rest)
        case Nil =>
          Stream.empty
      }
      entries = mergeValidatingBCs(entries).toList

      EntriesListStoreFactory.State(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean = {
      transaction.moneyReservoir == moneyReservoir
    }
  }

  def get(moneyReservoir: MoneyReservoir, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = moneyReservoir))
}

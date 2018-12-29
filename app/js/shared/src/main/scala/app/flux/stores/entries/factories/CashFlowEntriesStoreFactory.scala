package app.flux.stores.entries.factories

import app.common.money.ExchangeRateManager
import app.common.money.MoneyWithGeneralCurrency
import hydro.common.time.JavaTimeImplicits._
import hydro.common.time.LocalDateTime
import app.flux.stores.entries.CashFlowEntry.BalanceCorrection
import app.flux.stores.entries.CashFlowEntry.RegularEntry
import app.flux.stores.entries.WithIsPending.isAnyPending
import app.flux.stores.entries.WithIsPending.isPending
import app.flux.stores.entries.CashFlowEntry
import app.flux.stores.entries.WithIsPending
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.AppJsEntityAccess
import hydro.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.Transaction
import app.models.accounting._

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

final class CashFlowEntriesStoreFactory(implicit entityAccess: AppJsEntityAccess,
                                        accountingConfig: Config,
                                        exchangeRateManager: ExchangeRateManager)
    extends EntriesListStoreFactory[CashFlowEntry, MoneyReservoir] {

  override protected def createNew(maxNumEntries: Int, moneyReservoir: MoneyReservoir) = new Store {
    override protected def calculateState() = async {
      val oldestRelevantBalanceCheck: Option[BalanceCheck] = {
        val numTransactionsToFetch = 3 * maxNumEntries
        val totalNumTransactions =
          await(
            entityAccess
              .newQuery[Transaction]()
              .filter(ModelField.Transaction.moneyReservoirCode === moneyReservoir.code)
              .count())

        if (totalNumTransactions < numTransactionsToFetch) {
          None // get all entries

        } else {
          // get oldest oldestTransDate
          val oldestTransDate =
            await(
              entityAccess
                .newQuery[Transaction]()
                .filter(ModelField.Transaction.moneyReservoirCode === moneyReservoir.code)
                .sort(DbQuery.Sorting.Transaction.deterministicallyByTransactionDate.reversed)
                .limit(numTransactionsToFetch)
                .data()).last.transactionDate

          // get relevant balance checks
          await(
            entityAccess
              .newQuery[BalanceCheck]()
              .filter(ModelField.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
              .filter(ModelField.BalanceCheck.checkDate < oldestTransDate)
              .sort(DbQuery.Sorting.BalanceCheck.deterministicallyByCheckDate.reversed)
              .limit(1)
              .data()).headOption
        }
      }

      val oldestBalanceDate = oldestRelevantBalanceCheck.map(_.checkDate).getOrElse(LocalDateTime.MIN)
      val initialBalance =
        oldestRelevantBalanceCheck
          .map(_.balance)
          .getOrElse(MoneyWithGeneralCurrency(0, moneyReservoir.currency))

      val balanceChecksFuture: Future[Seq[BalanceCheck]] =
        entityAccess
          .newQuery[BalanceCheck]()
          .filter(ModelField.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
          .filter(ModelField.BalanceCheck.checkDate >= oldestBalanceDate)
          .data()

      // get relevant transactions
      val transactionsFuture: Future[Seq[Transaction]] =
        entityAccess
          .newQuery[Transaction]()
          .filter(ModelField.Transaction.moneyReservoirCode === moneyReservoir.code)
          .filter(ModelField.Transaction.transactionDate >= oldestBalanceDate)
          .data()
      val balanceChecks: Seq[BalanceCheck] = await(balanceChecksFuture)
      val transactions: Seq[Transaction] = await(transactionsFuture)

      // merge the two
      val mergedRows = (transactions ++ balanceChecks).sortBy {
        case trans: Transaction => (trans.transactionDate, trans.createdDate)
        case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
      }

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
            BalanceCorrection(bc, expectedAmount = currentBalance) #:: convertToEntries(rest, bc.balance)
          case Nil =>
            Stream.empty
        }
      var entries = convertToEntries(mergedRows.toList, initialBalance).toList

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
        case (regular: RegularEntry) :: BalanceCorrection(bc, _) :: rest if regular.balance == bc.balance =>
          mergeValidatingBCs(regular.copy(balanceVerified = true) :: rest)
        case entry :: rest =>
          entry #:: mergeValidatingBCs(rest)
        case Nil =>
          Stream.empty
      }
      entries = mergeValidatingBCs(entries).toList

      EntriesListStoreFactory.State(
        entries.takeRight(maxNumEntries).map(addIsPending),
        hasMore = entries.size > maxNumEntries,
        impactingTransactionIds = transactions.toStream.map(_.id).toSet,
        impactingBalanceCheckIds = (balanceChecks.toStream ++ oldestRelevantBalanceCheck).map(_.id).toSet
      )
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      transaction.moneyReservoir == moneyReservoir

    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) =
      balanceCheck.moneyReservoir == moneyReservoir

    private def addIsPending(entry: CashFlowEntry): WithIsPending[CashFlowEntry] = entry match {
      case e: RegularEntry      => WithIsPending(e, isPending = isAnyPending(e.transactions))
      case e: BalanceCorrection => WithIsPending(e, isPending = isPending(e.balanceCheck))
    }
  }

  def get(moneyReservoir: MoneyReservoir, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = moneyReservoir))
}

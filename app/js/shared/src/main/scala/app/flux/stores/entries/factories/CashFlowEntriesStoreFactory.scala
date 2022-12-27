package app.flux.stores.entries.factories

import app.common.money.CurrencyValueManager
import app.common.money.MoneyWithGeneralCurrency
import app.flux.stores.entries.AccountingEntryUtils
import app.flux.stores.entries.CashFlowEntry.BalanceCorrection
import app.flux.stores.entries.CashFlowEntry.RegularEntry
import app.flux.stores.entries.CashFlowEntry
import app.flux.stores.entries.WithIsPending
import app.flux.stores.entries.WithIsPending.isAnyPending
import app.flux.stores.entries.WithIsPending.isPending
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory.CashFlowAdditionalInput
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting.Transaction
import app.models.accounting._
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import hydro.common.time.JavaTimeImplicits._
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQueryImplicits._

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class CashFlowEntriesStoreFactory(implicit
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    currencyValueManager: CurrencyValueManager,
    accountingEntryUtils: AccountingEntryUtils,
) extends EntriesListStoreFactory[CashFlowEntry, CashFlowAdditionalInput] {

  override protected def createNew(maxNumEntries: Int, additionalInput: CashFlowAdditionalInput) = new Store {
    override protected def calculateState() = async {
      val oldestRelevantBalanceCheck: Option[BalanceCheck] = {
        val numTransactionsToFetch = 3 * maxNumEntries
        val totalNumTransactions =
          await(
            entityAccess
              .newQuery[Transaction]()
              .filter(ModelFields.Transaction.moneyReservoirCode === reservoir.code)
              .count()
          )

        if (totalNumTransactions < numTransactionsToFetch) {
          None // get all entries

        } else {
          // get oldest oldestTransDate
          val oldestTransDate =
            await(
              entityAccess
                .newQuery[Transaction]()
                .filter(ModelFields.Transaction.moneyReservoirCode === reservoir.code)
                .sort(AppDbQuerySorting.Transaction.deterministicallyByTransactionDate.reversed)
                .limit(numTransactionsToFetch)
                .data()
            ).last.transactionDate

          // get relevant balance checks
          await(
            entityAccess
              .newQuery[BalanceCheck]()
              .filter(ModelFields.BalanceCheck.moneyReservoirCode === reservoir.code)
              .filter(ModelFields.BalanceCheck.checkDate < oldestTransDate)
              .sort(AppDbQuerySorting.BalanceCheck.deterministicallyByCheckDate.reversed)
              .limit(1)
              .data()
          ).headOption
        }
      }

      val transactionsAndBalanceChecks =
        await(
          accountingEntryUtils.getTransactionsAndBalanceChecks(
            reservoir = reservoir,
            oldestRelevantBalanceCheck = oldestRelevantBalanceCheck,
          )
        )

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(
          nextRows: List[AnyRef],
          currentBalance: MoneyWithGeneralCurrency,
      ): Stream[CashFlowEntry] =
        (nextRows: @unchecked) match {
          case (trans: Transaction) :: rest =>
            val newBalance = currentBalance + trans.flow
            RegularEntry(List(trans), newBalance, balanceVerified = false) #:: convertToEntries(
              rest,
              newBalance,
            )
          case (bc: BalanceCheck) :: rest =>
            BalanceCorrection(bc, expectedAmount = currentBalance) #:: convertToEntries(rest, bc.balance)
          case Nil =>
            Stream.empty
        }
      var entries = convertToEntries(
        transactionsAndBalanceChecks.mergedRows.toList,
        transactionsAndBalanceChecks.initialBalance,
      ).toList

      // combine entries of same group and merge BC's with same balance (recursion does not lead to growing stack because of Stream)
      def combineSimilar(nextEntries: List[CashFlowEntry]): Stream[CashFlowEntry] = nextEntries match {
        case (x: RegularEntry) :: (y: RegularEntry) :: rest if x.groupId == y.groupId =>
          combineSimilar(
            RegularEntry(x.transactions ++ y.transactions, y.balance, balanceVerified = false) :: rest
          )
        case (x: BalanceCorrection) :: (y: BalanceCorrection) :: rest
            if x.balanceCheck.balance == y.balanceCheck.balance && compactBalanceChecks =>
          combineSimilar(x :: rest)
        case entry :: rest =>
          entry #:: combineSimilar(rest)
        case Nil =>
          Stream.empty
      }
      entries = combineSimilar(entries).toList

      if (compactBalanceChecks) {
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
      }

      EntriesListStoreFactory.State(
        entries.takeRight(maxNumEntries).map(addIsPending),
        hasMore = entries.size > maxNumEntries,
        impactingTransactionIds = transactionsAndBalanceChecks.impactingTransactionIds,
        impactingBalanceCheckIds = transactionsAndBalanceChecks.impactingBalanceCheckIds,
      )
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      transaction.moneyReservoir == reservoir

    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) =
      balanceCheck.moneyReservoir == reservoir

    private def addIsPending(entry: CashFlowEntry): WithIsPending[CashFlowEntry] = entry match {
      case e: RegularEntry      => WithIsPending(e, isPending = isAnyPending(e.transactions))
      case e: BalanceCorrection => WithIsPending(e, isPending = isPending(e.balanceCheck))
    }

    private def reservoir: MoneyReservoir = {
      additionalInput.reservoir
    }
    private def compactBalanceChecks: Boolean = {
      !additionalInput.showAllBalanceChecks
    }
  }

  def get(reservoir: MoneyReservoir, maxNumEntries: Int, showAllBalanceChecks: Boolean = false): Store = {
    get(
      Input(
        maxNumEntries = maxNumEntries,
        additionalInput =
          CashFlowAdditionalInput(reservoir = reservoir, showAllBalanceChecks = showAllBalanceChecks),
      )
    )
  }
}
object CashFlowEntriesStoreFactory {
  case class CashFlowAdditionalInput(
      reservoir: MoneyReservoir,
      showAllBalanceChecks: Boolean,
  )
}

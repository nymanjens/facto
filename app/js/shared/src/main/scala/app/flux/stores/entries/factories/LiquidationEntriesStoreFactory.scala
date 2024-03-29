package app.flux.stores.entries.factories

import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import app.common.money.CurrencyValueManager
import app.common.money.ReferenceMoney
import app.flux.stores.entries.WithIsPending.isAnyPending
import app.flux.stores.entries._
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class LiquidationEntriesStoreFactory(implicit
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    currencyValueManager: CurrencyValueManager,
) extends EntriesListStoreFactory[LiquidationEntry, AccountPair] {

  override protected def createNew(maxNumEntries: Int, accountPair: AccountPair) = new Store {
    override protected def calculateState() = async {
      val relevantTransactions = await(getRelevantTransactions())

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(
          nextTransactions: List[Transaction],
          currentDebt: ReferenceMoney,
      ): Stream[LiquidationEntry] =
        nextTransactions match {
          case trans :: rest =>
            val addsTo1To2Debt = trans.beneficiary == accountPair.account2
            val flow = trans.flow.exchangedForReferenceCurrency()
            val newDebt = if (addsTo1To2Debt) currentDebt + flow else currentDebt - flow
            LiquidationEntry(Seq(trans), newDebt) #:: convertToEntries(rest, newDebt)
          case Nil =>
            Stream.empty
        }
      var entries =
        convertToEntries(relevantTransactions.toList, ReferenceMoney(0) /* initial debt */ ).toList

      entries = GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
        /* combine */
        (first, last) =>
          LiquidationEntry(first.transactions ++ last.transactions, last.debt)
      }

      EntriesListStoreFactory.State(
        entries
          .takeRight(maxNumEntries)
          .map(entry => WithIsPending(entry, isPending = isAnyPending(entry.transactions))),
        hasMore = entries.size > maxNumEntries,
        impactingTransactionIds = relevantTransactions.toStream.map(_.id).toSet,
        impactingBalanceCheckIds = Set(),
      )
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      isRelevantForAccountsSync(transaction, accountPair)
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false

    private def getRelevantTransactions(): Future[Seq[Transaction]] = async {
      def reservoirsOwnedBy(account: Account): Seq[MoneyReservoir] = {
        accountingConfig.moneyReservoirs(includeHidden = true).filter(r => r.owner == account)
      }

      val account1ReservoirCodes = reservoirsOwnedBy(accountPair.account1).map(_.code)
      val account2ReservoirCodes = reservoirsOwnedBy(accountPair.account2).map(_.code)
      val transactions = await(
        entityAccess
          .newQuery[Transaction]()
          .filter(
            DbQuery.Filter.NullFilter[Transaction]()
              ||
                ((ModelFields.Transaction.moneyReservoirCode isAnyOf account1ReservoirCodes) &&
                  (ModelFields.Transaction.beneficiaryAccountCode === accountPair.account2.code))
                ||
                ((ModelFields.Transaction.moneyReservoirCode isAnyOf account2ReservoirCodes) &&
                  (ModelFields.Transaction.beneficiaryAccountCode === accountPair.account1.code))
                ||
                ((ModelFields.Transaction.moneyReservoirCode === "") &&
                  (ModelFields.Transaction.beneficiaryAccountCode isAnyOf accountPair.toSet
                    .map(_.code)
                    .toVector))
          )
          .sort(AppDbQuerySorting.Transaction.deterministicallyByTransactionDate)
          .data()
      )

      val isRelevantSeq = await(Future.sequence(transactions.map(isRelevantForAccounts(_, accountPair))))
      (isRelevantSeq zip transactions).filter(_._1).map(_._2)
    }

    private def isRelevantForAccounts(transaction: Transaction, accountPair: AccountPair): Future[Boolean] =
      async {
        val moneyReservoirOwner = transaction.moneyReservoirCode match {
          case "" =>
            // Pick the first beneficiary in the group. This simulates that the zero sum transaction was physically
            // performed on an actual reservoir, which is needed for the liquidation calculator to work.
            getOnlyElement(
              await(
                entityAccess
                  .newQuery[Transaction]()
                  .filter(ModelFields.Transaction.transactionGroupId === transaction.transactionGroupId)
                  .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
                  .limit(1)
                  .data()
              )
            ).beneficiary
          case _ => transaction.moneyReservoir.owner
        }
        val involvedAccounts: Set[Account] = Set(transaction.beneficiary, moneyReservoirOwner)
        accountPair.toSet == involvedAccounts
      }

    private def isRelevantForAccountsSync(transaction: Transaction, accountPair: AccountPair): Boolean = {
      if (transaction.moneyReservoirCode == "") {
        true // Heuristic
      } else {
        val moneyReservoirOwner = transaction.moneyReservoir.owner
        val involvedAccounts: Set[Account] = Set(transaction.beneficiary, moneyReservoirOwner)
        accountPair.toSet == involvedAccounts
      }
    }
  }

  def get(accountPair: AccountPair, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = accountPair))
}

package app.flux.stores.entries.factories

import app.common.GuavaReplacement.Iterables.getOnlyElement
import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import app.flux.stores.entries.WithIsPending.isAnyPending
import app.flux.stores.entries._
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.AppJsEntityAccess
import hydro.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

final class LiquidationEntriesStoreFactory(implicit entityAccess: AppJsEntityAccess,
                                           accountingConfig: Config,
                                           exchangeRateManager: ExchangeRateManager)
    extends EntriesListStoreFactory[LiquidationEntry, AccountPair] {

  override protected def createNew(maxNumEntries: Int, accountPair: AccountPair) = new Store {
    override protected def calculateState() = async {
      val relevantTransactions = await(getRelevantTransactions())

      // convert to entries (recursion does not lead to growing stack because of Stream)
      def convertToEntries(nextTransactions: List[Transaction],
                           currentDebt: ReferenceMoney): Stream[LiquidationEntry] =
        nextTransactions match {
          case trans :: rest =>
            val addsTo1To2Debt = trans.beneficiary == accountPair.account2
            val flow = trans.flow.exchangedForReferenceCurrency
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
        impactingBalanceCheckIds = Set()
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
                ((ModelField.Transaction.moneyReservoirCode isAnyOf account1ReservoirCodes) &&
                  (ModelField.Transaction.beneficiaryAccountCode === accountPair.account2.code))
              ||
                ((ModelField.Transaction.moneyReservoirCode isAnyOf account2ReservoirCodes) &&
                  (ModelField.Transaction.beneficiaryAccountCode === accountPair.account1.code))
              ||
                ((ModelField.Transaction.moneyReservoirCode === "") &&
                  (ModelField.Transaction.beneficiaryAccountCode isAnyOf accountPair.toSet
                    .map(_.code)
                    .toVector))
          )
          .sort(DbQuery.Sorting.Transaction.deterministicallyByTransactionDate)
          .data())

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
                  .filter(ModelField.Transaction.transactionGroupId === transaction.transactionGroupId)
                  .sort(DbQuery.Sorting.Transaction.deterministicallyByCreateDate)
                  .limit(1)
                  .data())).beneficiary
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

package flux.stores.entries

import common.money.{ExchangeRateManager, ReferenceMoney}
import models.EntityAccess
import models.access.DbQueryImplicits._
import models.access.{DbQuery, ModelField, JsEntityAccess}
import models.accounting.config.{Account, Config, MoneyReservoir}
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

final class LiquidationEntriesStoreFactory(implicit database: JsEntityAccess,
                                           accountingConfig: Config,
                                           exchangeRateManager: ExchangeRateManager,
                                           entityAccess: EntityAccess)
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
        entries.takeRight(maxNumEntries),
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
        database
          .newQuery[Transaction]()
          .filter(
            DbQuery.Filter.NullFilter[Transaction]()
              ||
                ((ModelField.Transaction.moneyReservoirCode isAnyOf account1ReservoirCodes) &&
                  (ModelField.Transaction.beneficiaryAccountCode isEqualTo accountPair.account2.code))
              ||
                ((ModelField.Transaction.moneyReservoirCode isAnyOf account2ReservoirCodes) &&
                  (ModelField.Transaction.beneficiaryAccountCode isEqualTo accountPair.account1.code))
              ||
                ((ModelField.Transaction.moneyReservoirCode isEqualTo "") &&
                  (ModelField.Transaction.beneficiaryAccountCode isAnyOf accountPair.toSet
                    .map(_.code)
                    .toVector))
          )
          .sort(DbQuery.Sorting
            .ascBy(ModelField.Transaction.transactionDate)
            .thenAscBy(ModelField.Transaction.createdDate)
            .thenAscBy(ModelField.id))
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
            await(Transaction.findByGroupId(transaction.transactionGroupId)).head.beneficiary
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

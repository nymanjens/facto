package flux.stores.entries

import jsfacades.Loki
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.accounting.config.{Account, Config}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq
import scala2js.Keys

final class LiquidationEntriesStoreFactory(implicit database: RemoteDatabaseProxy,
                                           accountingConfig: Config,
                                           exchangeRateManager: ExchangeRateManager,
                                           entityAccess: EntityAccess)
    extends EntriesListStoreFactory[LiquidationEntry, AccountPair] {

  override protected def createNew(maxNumEntries: Int, accountPair: AccountPair) = new Store {
    override protected def calculateState() = {
      val allTransactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .sort(
            Loki.Sorting
              .ascBy(Keys.Transaction.transactionDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()

      val relevantTransactions =
        for {
          transaction <- allTransactions
          if isRelevantForAccounts(transaction, accountPair)
        } yield transaction

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

      EntriesListStoreFactory.State(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean = {
      isRelevantForAccounts(transaction, accountPair)
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
  }

  def get(accountPair: AccountPair, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = accountPair))
}

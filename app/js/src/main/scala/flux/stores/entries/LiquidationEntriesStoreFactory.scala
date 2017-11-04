package flux.stores.entries

import common.LoggingUtils.logExceptions
import jsfacades.LokiJs
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.accounting.config.{Account, Config, MoneyReservoir}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class LiquidationEntriesStoreFactory(implicit database: RemoteDatabaseProxy,
                                           accountingConfig: Config,
                                           exchangeRateManager: ExchangeRateManager,
                                           entityAccess: EntityAccess)
    extends EntriesListStoreFactory[LiquidationEntry, AccountPair] {

  override protected def createNew(maxNumEntries: Int, accountPair: AccountPair) =
    new TransactionsListStore[LiquidationEntry] {
      override protected def calculateState() = logExceptions {
        val relevantTransactions = getRelevantTransactions()

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

        EntriesListStoreFactory.State.withImpactingIdsInEntries(
          entries.takeRight(maxNumEntries),
          hasMore = entries.size > maxNumEntries)
      }

      override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
        isRelevantForAccounts(transaction, accountPair)

      private def getRelevantTransactions() = {
        def reservoirsOwnedBy(account: Account): Seq[MoneyReservoir] = {
          accountingConfig.moneyReservoirs(includeHidden = true).filter(r => r.owner == account)
        }

        val transactions = database
          .newQuery[Transaction]()
          .filter(LokiJs.Filter.or(
            LokiJs.Filter.and(
              LokiJs.Filter.anyOf(
                Keys.Transaction.moneyReservoirCode,
                reservoirsOwnedBy(accountPair.account1).map(_.code)),
              LokiJs.Filter
                .equal(Keys.Transaction.beneficiaryAccountCode, accountPair.account2.code)
            ),
            LokiJs.Filter.and(
              LokiJs.Filter.anyOf(
                Keys.Transaction.moneyReservoirCode,
                reservoirsOwnedBy(accountPair.account2).map(_.code)),
              LokiJs.Filter
                .equal(Keys.Transaction.beneficiaryAccountCode, accountPair.account1.code)
            ),
            LokiJs.Filter.and(
              LokiJs.Filter.equal(Keys.Transaction.moneyReservoirCode, ""),
              LokiJs.Filter
                .anyOf(Keys.Transaction.beneficiaryAccountCode, accountPair.toSet.map(_.code).toVector)
            )
          ))
          .sort(
            LokiJs.Sorting
              .ascBy(Keys.Transaction.transactionDate)
              .thenAscBy(Keys.Transaction.createdDate)
              .thenAscBy(Keys.id))
          .data()

        transactions.filter(isRelevantForAccounts(_, accountPair))
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

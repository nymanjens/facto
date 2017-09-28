package flux.stores.entries

import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}
import models.accounting.config.{Account, Config}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class EndowmentEntriesStoreFactory(implicit database: RemoteDatabaseProxy, accountingConfig: Config)
    extends EntriesListStoreFactory[GeneralEntry, Account] {

  override protected def createNew(maxNumEntries: Int, account: Account) =
    new TransactionsListStore[GeneralEntry] {
      override protected def calculateState() = {
        val transactions: Seq[Transaction] =
          database
            .newQuery[Transaction]()
            .filter(Keys.Transaction.categoryCode, accountingConfig.constants.endowmentCategory.code)
            .filter(Keys.Transaction.beneficiaryAccountCode, account.code)
            .sort(
              LokiJs.Sorting
                .descBy(Keys.Transaction.consumedDate)
                .thenDescBy(Keys.Transaction.createdDate)
                .thenDescBy(Keys.id))
            .limit(3 * maxNumEntries)
            .data()
            .reverse

        var entries = transactions.map(t => GeneralEntry(Seq(t)))

        entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

        EntriesListStoreFactory.State(
          entries.takeRight(maxNumEntries),
          hasMore = entries.size > maxNumEntries)
      }

      override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
        transaction.category == accountingConfig.constants.endowmentCategory && transaction.beneficiary == account
    }

  def get(account: Account, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = account))
}

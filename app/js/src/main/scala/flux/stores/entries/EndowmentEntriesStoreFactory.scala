package flux.stores.entries

import models.access.DbQueryImplicits._
import models.access.{DbQuery, Fields, RemoteDatabaseProxy}
import models.accounting.config.{Account, Config}
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._

final class EndowmentEntriesStoreFactory(implicit database: RemoteDatabaseProxy, accountingConfig: Config)
    extends EntriesListStoreFactory[GeneralEntry, Account] {

  override protected def createNew(maxNumEntries: Int, account: Account) = new Store {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(
            Fields.Transaction.categoryCode isEqualTo accountingConfig.constants.endowmentCategory.code)
          .filter(Fields.Transaction.beneficiaryAccountCode isEqualTo account.code)
          .sort(
            DbQuery.Sorting
              .descBy(Fields.Transaction.consumedDate)
              .thenDescBy(Fields.Transaction.createdDate)
              .thenDescBy(Fields.id))
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State
        .withImpactingIdsInEntries(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      transaction.category == accountingConfig.constants.endowmentCategory && transaction.beneficiary == account
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  def get(account: Account, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = account))
}

package flux.stores.entries

import models.access.DbQueryImplicits._
import models.access.{DbQuery, ModelField, JsEntityAccess}
import models.accounting.config.{Account, Config}
import models.accounting.{BalanceCheck, Transaction}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

final class EndowmentEntriesStoreFactory(implicit entityAccess: JsEntityAccess, accountingConfig: Config)
    extends EntriesListStoreFactory[GeneralEntry, Account] {

  override protected def createNew(maxNumEntries: Int, account: Account) = new Store {
    override protected def calculateState() = async {
      val transactions: Seq[Transaction] =
        await(
          entityAccess
            .newQuery[Transaction]()
            .filter(ModelField.Transaction.categoryCode === accountingConfig.constants.endowmentCategory.code)
            .filter(ModelField.Transaction.beneficiaryAccountCode === account.code)
            .sort(DbQuery.Sorting.Transaction.deterministicallyByConsumedDate.reversed)
            .limit(3 * maxNumEntries)
            .data()).reverse

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

package app.flux.stores.entries.factories

import app.flux.stores.entries.GeneralEntry
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

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

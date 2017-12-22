package flux.stores.entries

import models.access.{DbQuery, Fields, RemoteDatabaseProxy}
import models.accounting.{BalanceCheck, Transaction}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AllEntriesStoreFactory(implicit database: RemoteDatabaseProxy)
    extends EntriesListStoreFactory[GeneralEntry, Unit] {

  override protected def createNew(maxNumEntries: Int, input: Unit) = new Store {
    override protected def calculateState() = async {
      val transactions: Seq[Transaction] =
        await(
          database
            .newQuery[Transaction]()
            .sort(
              DbQuery.Sorting
                .descBy(Fields.Transaction.transactionDate)
                .thenDescBy(Fields.Transaction.createdDate)
                .thenDescBy(Fields.id))
            .limit(3 * maxNumEntries)
            .data()).reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State
        .withImpactingIdsInEntries(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) = true
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  def get(maxNumEntries: Int): Store = get(Input(maxNumEntries = maxNumEntries, additionalInput = (): Unit))
}

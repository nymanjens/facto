package flux.stores.entries

import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}
import models.manager.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala2js.Keys

final class AllEntriesStoreFactory(implicit database: RemoteDatabaseProxy)
    extends EntriesListStoreFactory[GeneralEntry, Unit] {

  override protected def createNew(maxNumEntries: Int, input: Unit) =
    new TransactionsListStore[GeneralEntry] {
      override protected def calculateState() = {
        val transactions: Seq[Transaction] =
          database
            .newQuery[Transaction]()
            .sort(
              LokiJs.Sorting
                .descBy(Keys.Transaction.transactionDate)
                .thenDescBy(Keys.Transaction.createdDate)
                .thenDescBy(Keys.id))
            .limit(3 * maxNumEntries)
            .data()
            .reverse

        var entries = transactions.map(t => GeneralEntry(Seq(t)))

        entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

        EntriesListStoreFactory.State.withImpactingIdsInEntries(
          entries.takeRight(maxNumEntries),
          hasMore = entries.size > maxNumEntries)
      }

      override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) = true
    }

  def get(maxNumEntries: Int): Store = get(Input(maxNumEntries = maxNumEntries, additionalInput = (): Unit))
}

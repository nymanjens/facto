package flux.stores

import flux.stores.entries.GeneralEntry
import jsfacades.Loki
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.manager.{EntityModification, EntityType}

import scala.collection.immutable.Seq

final class AllEntriesStoreFactory(implicit database: RemoteDatabaseProxy)
  extends EntriesListStoreFactory[GeneralEntry, Unit] {

  override protected def createNew(maxNumEntries: Int, input: Unit) = new Store {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database.newQuery[Transaction]()
          .sort(Loki.Sorting.by("transactionDate").desc()
            .thenBy("createdDate").desc()
            .thenBy("id").asc())
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def modificationImpactsState(entityModification: EntityModification, state: State): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }
  }

  def get(maxNumEntries: Int): Store = get(Input(maxNumEntries = maxNumEntries, additionalInput = (): Unit))
}

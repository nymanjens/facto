package stores

import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.manager.{EntityModification, EntityType}
import stores.LastNEntriesStoreFactory.LastNEntriesState
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq

final class LastNEntriesStoreFactory(implicit database: RemoteDatabaseProxy)
  extends EntriesStoreFactory[LastNEntriesState] {

  override protected type Input = Int

  override protected def createNew(n: Int) = new EntriesStore[LastNEntriesState] {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database.newQuery[Transaction]()
          .sort("transactionDate", isDesc = true)
          .sort("createdDate", isDesc = true)
          .limit(3 * n)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      LastNEntriesState(entries.takeRight(n))
    }

    override protected def modificationImpactsState(entityModification: EntityModification, state: LastNEntriesState): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }
  }
}

object LastNEntriesStoreFactory {
  case class LastNEntriesState(entries: Seq[GeneralEntry])
}

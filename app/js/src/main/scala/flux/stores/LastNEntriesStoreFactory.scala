package flux.stores

import flux.stores.entries.GeneralEntry
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.manager.{EntityModification, EntityType}
import flux.stores.LastNEntriesStoreFactory.{LastNEntriesState, N}

import scala.collection.immutable.Seq

final class LastNEntriesStoreFactory(implicit database: RemoteDatabaseProxy)
  extends EntriesStoreFactory[LastNEntriesState] {

  override protected type Input = N

  override protected def createNew(n: N) = new EntriesStore[LastNEntriesState] {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database.newQuery[Transaction]()
          .sort("transactionDate", isDesc = true)
          .sort("createdDate", isDesc = true)
          .limit(3 * n.toInt)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      LastNEntriesState(entries.takeRight(n.toInt), hasMore = entries.size > n.toInt)
    }

    override protected def modificationImpactsState(entityModification: EntityModification, state: LastNEntriesState): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }
  }
}

object LastNEntriesStoreFactory {
  case class LastNEntriesState(entries: Seq[GeneralEntry], hasMore: Boolean)
  object LastNEntriesState {
    val empty: LastNEntriesState = LastNEntriesState(Seq(), false)
  }
  case class N(n: Int) {
    def toInt: Int = n
  }
}

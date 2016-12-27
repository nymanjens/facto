package stores

import api.ScalaJsApi.EntityType
import flux.Action.AddTransactionGroup
import flux.{Action, Dispatcher}
import models.access.{EntityModification, RemoteDatabaseProxy}
import models.accounting.Transaction
import stores.TransactionAndGroupStore.LastNEntriesState
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     dispatcher: Dispatcher) {
  dispatcher.register(actionCallback)

  def actionCallback: PartialFunction[Action, Unit] = {
    case AddTransactionGroup(transactionsWithoutId) => ???
  }

  def lastNEntriesStore(n: Int, registerListener: EntriesStore.Listener): EntriesStore[LastNEntriesState] =
    new EntriesStore[LastNEntriesState](registerListener) {
      override protected def calculateState(oldState: Option[LastNEntriesState]) = {
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

object TransactionAndGroupStore {
  case class LastNEntriesState(entries: Seq[GeneralEntry])
}

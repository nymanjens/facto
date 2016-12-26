package stores

import api.ScalaJsApi.EntityType
import flux.Action.AddTransactionGroup
import flux.{Action, Dispatcher}
import models.access.{EntityModification, RemoteDatabaseProxy}
import models.accounting.Transaction
import stores.entries.{GeneralEntry, GroupedTransactions}

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     dispatcher: Dispatcher) {
  dispatcher.register(actionCallback)

  def actionCallback(action: Action): Unit = action match {
    case AddTransactionGroup(transactionsWithoutId) => ???
  }

  def lastNEntriesStore(n: Int) = new LastNEntriesStore(n)

  final class LastNEntriesStore(n: Int) extends EntriesStore {
    override type State = ThisState

    override def calculateState() = {
      val transactions: Seq[Transaction] =
        database.newQuery[Transaction]()
          .sort("transactionDate", isDesc = true)
          .sort("createdDate", isDesc = true)
          .limit(3 * n)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      ThisState(entries.takeRight(n))
    }

    override def modificationImpactsState(entityModification: EntityModification): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }

    case class ThisState(entries: Seq[GeneralEntry])
  }
}

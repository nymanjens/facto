package stores

import api.ScalaJsApi.EntityType
import flux.Action.AddTransactionGroup
import flux.{Action, Dispatcher}
import models.access.{EntityModification, RemoteDatabaseProxy}
import models.accounting.Transaction
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     dispatcher: Dispatcher) {
  dispatcher.register(actionCallback)

  def actionCallback: PartialFunction[Action, Unit] = {
    case AddTransactionGroup(transactionsWithoutId) => ???
  }

  def lastNEntriesStore(n: Int) = new LastNEntriesStore(n)

  final class LastNEntriesStore(n: Int) extends EntriesStore {
    override type State = ThisState

    override def calculateState(oldState: Option[State]) = {
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

    override def modificationImpactsState(entityModification: EntityModification, state: State): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }

    case class ThisState(entries: Seq[GeneralEntry])
  }
}

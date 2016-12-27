package stores

import api.ScalaJsApi.EntityType
import common.time.Clock
import flux.Action.{AddTransactionGroup, RemoveTransactionGroup, UpdateTransactionGroup}
import flux.{Action, Dispatcher}
import models.EntityAccess
import models.access.{EntityModification, RemoteDatabaseProxy}
import models.accounting._
import stores.TransactionAndGroupStore.LastNEntriesState
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     entityAccess: EntityAccess,
                                     clock: Clock,
                                     dispatcher: Dispatcher) {
  dispatcher.register(actionCallback)

  def actionCallback: PartialFunction[Action, Unit] = {
    case AddTransactionGroup(transactionsWithoutIdProvider) =>
      val groupAddition = EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
      val group = groupAddition.entity
      val transactionsWithoutId = transactionsWithoutIdProvider(group)
      val transactionAdditions = transactionsWithoutId map (EntityModification.createAddWithRandomId(_))
      database.persistModifications(groupAddition +: transactionAdditions)

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      val transactionDeletions = group.transactions map (EntityModification.createDelete(_))
      val transactionAdditions = transactionsWithoutId map (EntityModification.createAddWithRandomId(_))
      database.persistModifications(transactionDeletions ++ transactionAdditions)

    case RemoveTransactionGroup(group) =>
      val transactionDeletions = group.transactions map (EntityModification.createDelete(_))
      val groupDeletion = EntityModification.createDelete(group)
      database.persistModifications(transactionDeletions :+ groupDeletion)
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

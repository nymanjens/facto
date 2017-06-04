package flux.stores

import common.time.Clock
import flux.action.Action.{AddTransactionGroup, RemoveTransactionGroup, UpdateTransactionGroup}
import flux.action.{Action, Dispatcher}
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.manager.EntityModification
import scala.collection.immutable.Seq

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     entityAccess: EntityAccess,
                                     clock: Clock,
                                     dispatcher: Dispatcher) {
  dispatcher.registerPartialAsync {
    case AddTransactionGroup(transactionsWithoutIdProvider) =>
      val groupAddition = EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
      val group = groupAddition.entity
      val transactionsWithoutId = transactionsWithoutIdProvider(group)
      val transactionAdditions =
        for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
          EntityModification.createAddWithId(transactionWithoutId, id)
        }
      database.persistModifications(groupAddition +: transactionAdditions)

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      val transactionDeletions = group.transactions map (EntityModification.createDelete(_))
      val transactionAdditions =
        for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
          EntityModification.createAddWithId(transactionWithoutId, id)
        }
      database.persistModifications(transactionDeletions ++ transactionAdditions)

    case RemoveTransactionGroup(group) =>
      val transactionDeletions = group.transactions map (EntityModification.createDelete(_))
      val groupDeletion = EntityModification.createDelete(group)
      database.persistModifications(transactionDeletions :+ groupDeletion)
  }

  private def zipWithIncrementingId[E](entities: Seq[E]): Seq[(E, Long)] = {
    val ids = {
      val start = EntityModification.generateRandomId()
      val end = start + entities.size
      start until end
    }
    entities zip ids
  }
}

package flux.stores

import common.time.Clock
import flux.action.Action.{AddTransactionGroup, RemoveTransactionGroup, UpdateTransactionGroup}
import flux.action.Dispatcher
import models.access.{EntityAccess, JsEntityAccess}
import models.accounting._
import models.modification.EntityModification

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] final class TransactionAndGroupStore(implicit database: JsEntityAccess,
                                                     entityAccess: EntityAccess,
                                                     clock: Clock,
                                                     dispatcher: Dispatcher) {
  dispatcher.registerPartialAsync {
    case AddTransactionGroup(transactionsWithoutIdProvider) =>
      async {
        val groupAddition =
          EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
        val group = groupAddition.entity
        val transactionsWithoutId = transactionsWithoutIdProvider(group)
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(transactionWithoutId, id)
          }
        await(database.persistModifications(groupAddition +: transactionAdditions))
      }

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      async {
        val transactionDeletions = await(group.withTransactions).transactions map (EntityModification
          .createDelete(_))
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(transactionWithoutId, id)
          }
        await(database.persistModifications(transactionDeletions ++ transactionAdditions))
      }

    case RemoveTransactionGroup(group) =>
      async {
        val transactionDeletions = await(group.withTransactions).transactions map (EntityModification
          .createDelete(_))
        val groupDeletion = EntityModification.createDelete(group)
        await(database.persistModifications(transactionDeletions :+ groupDeletion))
      }
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

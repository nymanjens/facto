package app.flux.stores

import app.flux.action.AppActions.AddTransactionGroup
import app.flux.action.AppActions.RemoveTransactionGroup
import app.flux.action.AppActions.UpdateTransactionGroup
import app.models.access.AppJsEntityAccess
import app.models.accounting._
import hydro.models.modification.EntityModification
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] final class TransactionAndGroupStore(
    implicit entityAccess: AppJsEntityAccess,
    clock: Clock,
    dispatcher: Dispatcher,
) {
  dispatcher.registerPartialAsync {
    case AddTransactionGroup(transactionsWithoutIdProvider) =>
      async {
        val groupAddition =
          EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
        val group = groupAddition.entity
        val transactionsWithoutId = transactionsWithoutIdProvider(group)
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(groupAddition +: transactionAdditions))
      }

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createRemove(_))
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(transactionDeletions ++ transactionAdditions))
      }

    case RemoveTransactionGroup(group) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createRemove(_))
        val groupDeletion = EntityModification.createRemove(group)
        await(entityAccess.persistModifications(transactionDeletions :+ groupDeletion))
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

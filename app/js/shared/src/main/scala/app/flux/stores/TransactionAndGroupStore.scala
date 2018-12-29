package app.flux.stores

import hydro.common.time.Clock
import app.flux.action.Actions.AddTransactionGroup
import app.flux.action.Actions.RemoveTransactionGroup
import app.flux.action.Actions.UpdateTransactionGroup
import hydro.flux.action.Dispatcher
import app.models.access.AppJsEntityAccess
import hydro.models.access.JsEntityAccess
import app.models.accounting._
import app.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] final class TransactionAndGroupStore(implicit entityAccess: AppJsEntityAccess,
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
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(groupAddition +: transactionAdditions))
      }

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createDelete(_))
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(transactionDeletions ++ transactionAdditions))
      }

    case RemoveTransactionGroup(group) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createDelete(_))
        val groupDeletion = EntityModification.createDelete(group)
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

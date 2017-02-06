package flux.stores

import common.time.Clock
import flux.action.Action.{AddTransactionGroup, RemoveTransactionGroup, UpdateTransactionGroup}
import flux.action.{Action, Dispatcher}
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.manager.EntityModification

final class TransactionAndGroupStore(implicit database: RemoteDatabaseProxy,
                                     entityAccess: EntityAccess,
                                     clock: Clock,
                                     dispatcher: Dispatcher) {
  dispatcher.register(actionCallback)

  private def actionCallback: PartialFunction[Action, Unit] = {
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
}

package flux.stores

import flux.action.Action.{AddBalanceCheck, RemoveBalanceCheck, UpdateBalanceCheck}
import flux.action.Dispatcher
import models.access.RemoteDatabaseProxy
import models.modification.EntityModification

private[stores] final class BalanceCheckStore(implicit database: RemoteDatabaseProxy, dispatcher: Dispatcher) {
  dispatcher.registerPartialAsync {
    case AddBalanceCheck(balanceCheckWithoutId) =>
      database.persistModifications(EntityModification.createAddWithRandomId(balanceCheckWithoutId))

    case UpdateBalanceCheck(existingBalanceCheck, newBalanceCheckWithoutId) =>
      val bcDeletion = EntityModification.createDelete(existingBalanceCheck)
      val bcAddition = EntityModification.createAddWithRandomId(newBalanceCheckWithoutId)
      database.persistModifications(bcDeletion, bcAddition)

    case RemoveBalanceCheck(balanceCheckWithId) =>
      database.persistModifications(EntityModification.createDelete(balanceCheckWithId))
  }
}

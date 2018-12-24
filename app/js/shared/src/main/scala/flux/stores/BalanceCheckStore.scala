package flux.stores

import flux.action.Actions.AddBalanceCheck
import flux.action.Actions.RemoveBalanceCheck
import flux.action.Actions.UpdateBalanceCheck
import hydro.flux.action.Dispatcher
import models.access.JsEntityAccess
import models.modification.EntityModification

private[stores] final class BalanceCheckStore(implicit entityAccess: JsEntityAccess, dispatcher: Dispatcher) {
  dispatcher.registerPartialAsync {
    case AddBalanceCheck(balanceCheckWithoutId) =>
      entityAccess.persistModifications(EntityModification.createAddWithRandomId(balanceCheckWithoutId))

    case UpdateBalanceCheck(existingBalanceCheck, newBalanceCheckWithoutId) =>
      val bcDeletion = EntityModification.createDelete(existingBalanceCheck)
      val bcAddition = EntityModification.createAddWithRandomId(newBalanceCheckWithoutId)
      entityAccess.persistModifications(bcDeletion, bcAddition)

    case RemoveBalanceCheck(balanceCheckWithId) =>
      entityAccess.persistModifications(EntityModification.createDelete(balanceCheckWithId))
  }
}

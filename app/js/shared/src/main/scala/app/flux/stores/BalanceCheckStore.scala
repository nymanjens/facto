package app.flux.stores

import app.flux.action.AppActions.AddBalanceCheck
import app.flux.action.AppActions.RemoveBalanceCheck
import app.flux.action.AppActions.UpdateBalanceCheck
import hydro.flux.action.Dispatcher
import app.models.access.AppJsEntityAccess
import hydro.models.access.JsEntityAccess
import app.models.modification.EntityModification

private[stores] final class BalanceCheckStore(implicit entityAccess: AppJsEntityAccess,
                                              dispatcher: Dispatcher) {
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

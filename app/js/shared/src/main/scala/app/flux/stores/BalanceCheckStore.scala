package app.flux.stores

import app.flux.action.AppActions.AddBalanceCheck
import app.flux.action.AppActions.RemoveBalanceCheck
import app.flux.action.AppActions.UpdateBalanceCheck
import app.models.access.AppJsEntityAccess
import hydro.models.modification.EntityModification
import hydro.flux.action.Dispatcher

private[stores] final class BalanceCheckStore(
    implicit entityAccess: AppJsEntityAccess,
    dispatcher: Dispatcher,
) {
  dispatcher.registerPartialAsync {
    case AddBalanceCheck(balanceCheckWithoutId) =>
      entityAccess.persistModifications(EntityModification.createAddWithRandomId(balanceCheckWithoutId))

    case UpdateBalanceCheck(existingBalanceCheck, newBalanceCheckWithoutId) =>
      val bcDeletion = EntityModification.createRemove(existingBalanceCheck)
      val bcAddition = EntityModification.createAddWithRandomId(newBalanceCheckWithoutId)
      entityAccess.persistModifications(bcDeletion, bcAddition)

    case RemoveBalanceCheck(balanceCheckWithId) =>
      entityAccess.persistModifications(EntityModification.createRemove(balanceCheckWithId))
  }
}

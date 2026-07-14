package app.flux.stores

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.flux.action.AppActions
import app.flux.action.AppActions.AddBalanceCheck
import hydro.flux.action.StandardActions.DoneWithLink.PageFactory
import app.flux.action.AppActions.RemoveBalanceCheck
import app.flux.action.AppActions.UpdateBalanceCheck
import app.flux.router.AppPages
import app.models.access.AppJsEntityAccess
import hydro.models.modification.EntityModification
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.flux.router.RouterContext

private[stores] final class BalanceCheckStore(implicit
    entityAccess: AppJsEntityAccess,
    dispatcher: Dispatcher,
) {
  dispatcher.registerPartialAsync {
    case action @ AddBalanceCheck(balanceCheckWithoutId) =>
      async {
        val bcWithId = EntityModification.createAddWithRandomId(balanceCheckWithoutId)
        await(entityAccess.persistModifications(bcWithId))
        dispatcher.dispatch(
          StandardActions.DoneWithLink(
            action,
            new PageFactory {
              def create()(implicit routerContext: RouterContext): Page =
                AppPages.EditBalanceCheck(bcWithId.entity)
            },
          )
        )
      }

    case action @ UpdateBalanceCheck(existingBalanceCheck, newBalanceCheckWithoutId) =>
      async {
        val bcDeletion = EntityModification.createRemove(existingBalanceCheck)
        val bcAddition = EntityModification.createAddWithRandomId(newBalanceCheckWithoutId)
        await(entityAccess.persistModifications(bcDeletion, bcAddition))
        dispatcher.dispatch(
          StandardActions.DoneWithLink(
            action,
            new PageFactory {
              def create()(implicit routerContext: RouterContext): Page =
                AppPages.EditBalanceCheck(bcAddition.entity)
            },
          )
        )
      }

    case RemoveBalanceCheck(balanceCheckWithId) =>
      entityAccess.persistModifications(EntityModification.createRemove(balanceCheckWithId))
  }
}

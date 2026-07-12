package hydro.flux.action

import app.api.ScalaJsApi.UserPrototype
import hydro.flux.action.StandardActions.DoneWithLink.PageFactory
import hydro.flux.router.Page
import hydro.flux.router.RouterContext

object StandardActions {

  // **************** General actions **************** //
  case class SetPageLoadingState(isLoading: Boolean) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after they processed the contained action. */
  case class Done private[action] (action: Action) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after processing an action failed. */
  case class Failed private[action] (action: Action) extends Action

  // **************** Link metadata **************** //
  case class DoneWithLink(action: Action, linkPage: PageFactory) extends Action
  object DoneWithLink {
    trait PageFactory {
      def create()(implicit routerContext: RouterContext): Page
    }
  }

  // **************** User-related actions **************** //
  case class UpsertUser(userPrototype: UserPrototype) extends Action
}

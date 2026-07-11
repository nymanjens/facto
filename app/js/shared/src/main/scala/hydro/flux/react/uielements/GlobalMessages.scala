package hydro.flux.react.uielements

import app.flux.router.AppPages
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.GlobalMessagesStore.Message
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

final class GlobalMessages(implicit globalMessagesStore: GlobalMessagesStore, i18n: I18n)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(globalMessagesStore, _.copy(maybeMessage = globalMessagesStore.state))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(maybeMessage: Option[GlobalMessagesStore.Message] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val _ = props.router
      state.maybeMessage match {
        case None => <.span()
        case Some(message) =>
          Bootstrap.Alert(variant = Variant.info)(
            ^.className := "global-messages",
            <.span(
              Bootstrap.Icon(iconClassName(message.messageType))(
                ^.style := js.Dictionary("marginRight" -> "11px")
              ),
              " ",
            ),
            message.string,
            message.linkPage.map { pageFactory =>
              <.span(
                " ",
                props.router.anchorWithHrefTo(pageFactory.create())(
                  <.span(
                    ^.className := "global-message-link",
                    s"[${i18n("app.edit")}]",
                  )
                ),
              )
            },
          )
      }
    }

    private def iconClassName(messageType: Message.Type): String = messageType match {
      case Message.Type.Working => "fa fa-circle-o-notch fa-spin"
      case Message.Type.Success => "fa fa-check"
      case Message.Type.Failure => "fa fa-warning"
    }
  }
}

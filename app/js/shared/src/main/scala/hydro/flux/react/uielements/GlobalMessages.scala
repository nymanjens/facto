package hydro.flux.react.uielements

import hydro.common.LoggingUtils.logExceptions
import app.flux.react.app.Menu
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.GlobalMessagesStore.Message
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

final class GlobalMessages(implicit globalMessagesStore: GlobalMessagesStore) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(globalMessagesStore, _.copy(maybeMessage = globalMessagesStore.state))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(maybeMessage: Option[GlobalMessagesStore.Message] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.maybeMessage match {
        case None => <.span()
        case Some(message) =>
          <.div(
            ^.className := "alert alert-info",
            ^.style := js.Dictionary("marginTop" -> "20px"),
            <.span(
              <.i(
                ^.className := iconClassNames(message.messageType),
                ^.style := js.Dictionary("marginRight" -> "11px")),
              " "),
            message.string
          )
      }
    }

    private def iconClassNames(messageType: Message.Type): String = messageType match {
      case Message.Type.Working => "fa fa-circle-o-notch fa-spin"
      case Message.Type.Success => "fa fa-check"
      case Message.Type.Failure => "fa fa-warning"
    }
  }
}

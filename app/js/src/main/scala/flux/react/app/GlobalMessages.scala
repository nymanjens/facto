package flux.react.app

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import flux.stores.GlobalMessagesStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

private[app] final class GlobalMessages(implicit globalMessagesStore: GlobalMessagesStore, menu: Menu) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(maybeMessage = None))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(): VdomElement = {
    component()
  }

  // **************** Private inner types ****************//
  private type Props = Unit
  private case class State(maybeMessage: Option[GlobalMessagesStore.Message]) {
    def withUpdatedMessage(implicit globalMessagesStore: GlobalMessagesStore): State = {
      copy(maybeMessage = globalMessagesStore.state)
    }
  }

  private class Backend($ : BackendScope[Props, State]) extends GlobalMessagesStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      globalMessagesStore.register(this)
      $.modState(state => logExceptions(state.withUpdatedMessage)).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      globalMessagesStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.withUpdatedMessage)).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      state.maybeMessage match {
        case None => <.span()
        case Some(message) =>
          <.div(
            ^.className := "alert alert-info",
            ^.style := js.Dictionary("marginTop" -> "20px"),
            ^^.ifThen(message.isWorking) {
              <.span(
                <.i(
                  ^.className := "fa fa-circle-o-notch fa-spin",
                  ^.style := js.Dictionary("marginRight" -> "11px")),
                " ")
            },
            message.string
          )
      }
    }
  }
}

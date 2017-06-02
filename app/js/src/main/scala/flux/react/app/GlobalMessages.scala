package flux.react.app

import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.{ReactElement, ReactNode}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._
import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import flux.react.router.Page
import flux.stores.GlobalMessagesStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

private[app] final class GlobalMessages(implicit globalMessagesStore: GlobalMessagesStore, menu: Menu) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](State(maybeMessage = None))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component()
  }

  // **************** Private inner types ****************//
  private type Props = Unit
  private case class State(maybeMessage: Option[GlobalMessagesStore.Message]) {
    def withUpdatedMessage(implicit globalMessagesStore: GlobalMessagesStore): State = {
      copy(maybeMessage = globalMessagesStore.state)
    }
  }

  private class Backend($: BackendScope[Props, State]) extends GlobalMessagesStore.Listener {

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

    def render(props: Props, state: State): ReactElement = logExceptions {
      state.maybeMessage match {
        case None => <.span()
        case Some(message) => <.div(message.string)
      }
    }
  }
}

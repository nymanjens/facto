package flux.react.app

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.stores.{PageLoadingStateStore, PendingModificationsStore, StateStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class PendingModificationsCounter(
    implicit pendingModificationsStore: PendingModificationsStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(numberOfModifications = 0))
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
  private case class State(numberOfModifications: Int)

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      pendingModificationsStore.register(this)
      $.modState(state =>
        State(numberOfModifications = pendingModificationsStore.state.numberOfModifications)).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      pendingModificationsStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state =>
        State(numberOfModifications = pendingModificationsStore.state.numberOfModifications)).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      state.numberOfModifications match {
        case 0 =>
          <.span()
        case numberOfModifications =>
          <.span(
            ^.className := "navbar-brand pending-modifications",
            <.i(^.className := "glyphicon-hourglass"),
            " ",
            numberOfModifications)
      }
    }
  }
}

package flux.react.app

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.stores.{ApplicationIsOnlineStore, StateStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class ApplicationDisconnectedIcon(
    implicit applicationIsOnlineStore: ApplicationIsOnlineStore,
    i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(isDisconnected = false))
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
  private case class State(isDisconnected: Boolean)

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      applicationIsOnlineStore.register(this)
      $.modState(state => State(isDisconnected = !applicationIsOnlineStore.state.isOnline)).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      applicationIsOnlineStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => State(isDisconnected = !applicationIsOnlineStore.state.isOnline)).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      state.isDisconnected match {
        case true =>
          <.span(
            ^.className := "navbar-brand",
            <.i(^.className := "fa fa-chain-broken", ^.title := i18n("facto.offline")))
        case false =>
          <.span()
      }
    }
  }
}

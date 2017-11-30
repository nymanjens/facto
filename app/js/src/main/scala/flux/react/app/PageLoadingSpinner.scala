package flux.react.app

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import flux.stores.PageLoadingStateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

private[app] final class PageLoadingSpinner(implicit pageLoadingStateStore: PageLoadingStateStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(isLoading = false))
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
  private case class State(isLoading: Boolean)

  private class Backend($ : BackendScope[Props, State]) extends PageLoadingStateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      pageLoadingStateStore.register(this)
      $.modState(state => State(isLoading = pageLoadingStateStore.state.isLoading)).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      pageLoadingStateStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => State(isLoading = pageLoadingStateStore.state.isLoading)).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      state.isLoading match {
        case true =>
          <.span(^.className := "navbar-brand", <.i(^.className := "fa fa-circle-o-notch fa-spin"))
        case false =>
          <.span()
      }
    }
  }
}

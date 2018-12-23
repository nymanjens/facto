package flux.react.app

import common.LoggingUtils.logExceptions
import flux.react.common.HydroReactComponent
import flux.stores.PageLoadingStateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class PageLoadingSpinner(implicit pageLoadingStateStore: PageLoadingStateStore)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      pageLoadingStateStore,
      _.copy(isLoading = pageLoadingStateStore.state.isLoading))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(isLoading: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.isLoading match {
        case true =>
          <.span(^.className := "navbar-brand", <.i(^.className := "fa fa-circle-o-notch fa-spin"))
        case false =>
          <.span()
      }
    }
  }
}

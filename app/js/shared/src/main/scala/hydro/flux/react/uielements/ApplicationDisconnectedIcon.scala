package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.stores.ApplicationIsOnlineStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class ApplicationDisconnectedIcon(
    implicit applicationIsOnlineStore: ApplicationIsOnlineStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      applicationIsOnlineStore,
      _.copy(isDisconnected = !applicationIsOnlineStore.state.isOnline))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(isDisconnected: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.isDisconnected match {
        case true =>
          Bootstrap.NavbarBrand()(
            Bootstrap.FontAwesomeIcon("chain-broken")(^.title := i18n("app.offline"))
          )
        case false =>
          <.span()
      }
    }
  }
}

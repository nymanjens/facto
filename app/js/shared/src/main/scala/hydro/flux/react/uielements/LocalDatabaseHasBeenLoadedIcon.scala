package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.stores.LocalDatabaseHasBeenLoadedStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class LocalDatabaseHasBeenLoadedIcon(
    implicit localDatabaseHasBeenLoadedStore: LocalDatabaseHasBeenLoadedStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      localDatabaseHasBeenLoadedStore,
      _.copy(hasBeenLoaded = localDatabaseHasBeenLoadedStore.state.hasBeenLoaded))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(hasBeenLoaded: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.hasBeenLoaded match {
        case true =>
          Bootstrap.NavbarBrand()(
            Bootstrap.FontAwesomeIcon("database")(^.title := i18n("app.local-database-has-been-loaded"))
          )
        case false =>
          <.span()
      }
    }
  }
}

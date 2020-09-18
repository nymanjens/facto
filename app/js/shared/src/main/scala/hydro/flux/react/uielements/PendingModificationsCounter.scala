package hydro.flux.react.uielements

import app.flux.stores.PendingModificationsStore
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class PendingModificationsCounter(implicit pendingModificationsStore: PendingModificationsStore)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      pendingModificationsStore,
      _.copy(numberOfModifications = pendingModificationsStore.state.numberOfModifications),
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(numberOfModifications: Int = 0)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.numberOfModifications match {
        case 0 =>
          <.span()
        case numberOfModifications =>
          Bootstrap.NavbarBrand()(
            ^.className := "pending-modifications",
            Bootstrap.Glyphicon("hourglass"),
            " ",
            numberOfModifications,
          )
      }
    }
  }
}

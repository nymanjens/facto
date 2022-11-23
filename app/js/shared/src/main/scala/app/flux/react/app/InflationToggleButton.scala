package app.flux.react.app

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.common.JsLoggingUtils.logExceptions
import app.flux.stores.InMemoryUserConfigFactory
import app.models.accounting.config.Config
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.LogExceptionsFuture
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

private[app] final class InflationToggleButton(implicit
    accountingConfig: Config,
    inMemoryUserConfigStore: InMemoryUserConfigFactory,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(
      Props()
    )
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      inMemoryUserConfigStore,
      _.copy(correctForInflationEnabled = inMemoryUserConfigStore.state.correctForInflation),
    )

  // **************** Private inner types ****************//
  protected case class Props(
  )
  protected case class State(correctForInflationEnabled: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State) = logExceptions {
      if (accountingConfig.constants.supportInflationCorrections) {
        <.div(
          <.i(^.className := "icon-inflation"),
          " ",
          <.span(
            ^.className := "facto-custom-switch-button",
            <.input(
              ^.tpe := "checkbox",
              ^.checked := state.correctForInflationEnabled,
              ^.readOnly := true,
            ),
            <.label(
              ^.className := "label-primary",
              ^.onClick --> {
                LogExceptionsFuture {
                  inMemoryUserConfigStore.mutateState(s =>
                    s.copy(correctForInflation = !s.correctForInflation)
                  )
                }
                $.modState(s => s.copy(correctForInflationEnabled = !s.correctForInflationEnabled))
              },
            ),
          ),
        )
      } else {
        VdomArray.empty()
      }
    }
  }
}

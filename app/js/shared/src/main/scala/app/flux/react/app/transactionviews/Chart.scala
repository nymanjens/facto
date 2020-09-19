package app.flux.react.app.transactionviews

import scala.collection.immutable.Seq
import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.ChartSpecInput.ChartSpec
import app.flux.react.app.transactionviews.ChartSpecInput.Line
import app.flux.router.AppPages
import app.flux.stores.entries.factories.ChartStoreFactory
import app.flux.stores.entries.factories.ChartStoreFactory.LinePoints
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class Chart(implicit
    summaryTable: SummaryTable,
    entityAccess: AppJsEntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
    chartSpecInput: ChartSpecInput,
    chartStoreFactory: ChartStoreFactory,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(stringifiedChartSpecs: String, router: RouterContext): VdomElement = {
    component(
      Props(
        chartSpec = ChartSpec.parseStringified(stringifiedChartSpecs),
        router = router,
      )
    )
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(
    backendConstructor = new Backend(_),
    initialState = State(),
    stateStoresDependencies = Some(props =>
      for (line <- props.chartSpec.lines) yield {
        val store = chartStoreFactory.get(line.query)
        StateStoresDependency(
          store,
          oldState => oldState.copy(lineToPoints = oldState.lineToPoints.updated(line, store.state)),
        )
      }
    ),
  )

  // **************** Private inner types ****************//
  protected case class Props(
      chartSpec: ChartSpec,
      router: RouterContext,
  )
  protected case class State(
      lineToPoints: Map[Line, LinePoints] = Map()
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.span(
        ^.className := "charts-page",
        pageHeader(router.currentPage),
        Bootstrap.Row(
          chartSpecInput(
            chartSpec = props.chartSpec,
            onChartSpecUpdate = newChartSpec => {
              router.setPage(AppPages.Chart.fromStringifiedChartSpec(newChartSpec.stringify))
              Callback.empty
            },
          )
        ),
      )
    }
  }
}

package app.flux.react.app.transactionviews

import scala.scalajs.js.JSConverters._
import java.time.Month

import app.common.money.Currency

import scala.collection.immutable.Seq
import app.common.money.ExchangeRateManager
import app.common.money.Money
import app.common.money.ReferenceMoney
import app.common.time.DatedMonth
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
import hydro.jsfacades.Recharts
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable
import scala.scalajs.js

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

  private val lineColors: Seq[String] =
    Seq("purple", "orange", "green", "deeppink", "#DD0", "fuchsia", "red", "blue")

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
      implicit val _ = props
      implicit val __ = state
      <.span(
        ^.className := "charts-page",
        pageHeader(router.currentPage),
        // **************** Chartspec **************** //
        Bootstrap.Row(
          chartSpecInput(
            chartSpec = props.chartSpec,
            onChartSpecUpdate = newChartSpec => {
              router.setPage(AppPages.Chart.fromStringifiedChartSpec(newChartSpec.stringify))
              Callback.empty
            },
          )
        ),
        // **************** Chart **************** //
        <.div(
          Recharts.ResponsiveContainer(width = "100%", height = 450)(
            Recharts.LineChart(
              data = assembleData(),
              margin = Recharts.Margin(top = 5, right = 50, left = 50, bottom = 35),
            )(
              Recharts.CartesianGrid(strokeDasharray = "3 3", vertical = false),
              Recharts.XAxis(
                dataKey = "month",
                tickFormatter = s => s.toString.takeRight(4),
                ticks = assembleAllJanuaries().toJSArray,
              ),
              Recharts.YAxis(tickFormatter = formatDoubleMoney(roundToInteger = true)),
              Recharts.Tooltip(formatter = formatDoubleMoney()),
              Recharts.Legend(),
              (for ((line, lineIndex) <- props.chartSpec.lines.zipWithIndex)
                yield Recharts.Line(
                  key = lineName(line, lineIndex),
                  tpe = "linear",
                  dataKey = lineName(line, lineIndex),
                  stroke = lineColors(lineIndex % lineColors.size),
                )).toVdomArray,
            )
          )
        ),
      )
    }

    private def assembleData()(implicit props: Props, state: State): Seq[Map[String, js.Any]] = {
      val cumulativeMap = mutable.Map[Line, ReferenceMoney]().withDefaultValue(ReferenceMoney(0))
      for (month <- getAllMonths()) yield {
        Map[String, js.Any](
          "month" -> formatMonth(month)
        ) ++ props.chartSpec.lines.zipWithIndex.map { case (line, lineIndex) =>
          lineName(line, lineIndex) -> {
            val amount =
              state
                .lineToPoints(line)
                .points
                .getOrElse(month, ReferenceMoney(0))
            val result = {
              if(line.cumulative) {
                val newCumulativeAmount = cumulativeMap(line) + amount
                cumulativeMap.put(line, newCumulativeAmount)
                newCumulativeAmount
              } else {
                amount
              }
            }
            (if (line.inverted) -result.toDouble else result.toDouble): js.Any
          }
        }
      }
    }

    private def assembleAllJanuaries()(implicit props: Props, state: State): Seq[String] = {
      getAllMonths().filter(_.month == Month.JANUARY).map(formatMonth)
    }

    private def getAllMonths()(implicit props: Props, state: State): Seq[DatedMonth] = {
      val allDatesWithData = state.lineToPoints.flatMap(_._2.points.keySet)
      if (allDatesWithData.isEmpty) {
        Seq()
      } else {
        DatedMonth.monthsInClosedRange(allDatesWithData.min, allDatesWithData.max)
      }
    }

    private def lineName(line: Line, lineIndex: Int): String = {
      s"Graph ${lineIndex + 1}: '${line.query}'"
    }

    private def formatMonth(month: DatedMonth): String = {
      s"${month.abbreviation} ${month.year}"
    }

    private def formatDoubleMoney(roundToInteger: Boolean = false)(amount: Any): String = {
      val money = ReferenceMoney(Money.floatToCents(amount.asInstanceOf[Double]))
      val moneyString = if (roundToInteger) money.formatFloat.dropRight(3) else money.formatFloat
      val nonBreakingSpace = "\u00A0"
      s"${money.currency.symbol}${nonBreakingSpace}$moneyString"
    }
  }
}

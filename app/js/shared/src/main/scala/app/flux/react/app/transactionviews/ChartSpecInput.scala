package app.flux.react.app.transactionviews

import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.ChartSpecInput.ChartSpec
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.Table
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class ChartSpecInput(implicit
    i18n: I18n
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      chartSpec: ChartSpec,
      onChartSpecUpdate: ChartSpec => Callback,
  ): VdomElement = {
    component(Props(chartSpec, onChartSpecUpdate))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Private inner types ****************//
  protected case class Props(
      chartSpec: ChartSpec,
      onChartSpecUpdate: ChartSpec => Callback,
  ) {
    def notifyUpdatedChartSpec(modification: ChartSpec => ChartSpec): Callback = {
      onChartSpecUpdate(modification(chartSpec))
    }
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State) = logExceptions {
      implicit val _: Props = props
      Panel(title = i18n("app.graph-lines"), lg = 8) {
        <.span(
          Table(
            tableClasses = Seq("table-chart-spec"),
            tableHeaders = Seq(
              <.th(i18n("app.query"), ^.width := "100%"),
              <.th(i18n("app.inverted")),
              <.th(i18n("app.cumulative")),
              <.th(),
            ),
            tableRowDatas = tableRowDatas,
          ),
          addButton,
        )
      }
    }

    private def tableRowDatas(implicit props: Props): Seq[Table.TableRowData] = {
      for ((line, lineIndex) <- props.chartSpec.lines.zipWithIndex) yield {
        Table.TableRowData(
          Seq[VdomElement](
            <.td(
              SingleTextInputForm(
                defaultValue = line.query,
                onChange =
                  newQuery => props.notifyUpdatedChartSpec(_.modified(lineIndex, _.copy(query = newQuery))),
              )
            ),
            <.td(
              <.input(
                ^.tpe := "checkbox",
                ^.checked := line.inverted,
                ^.onChange --> props.notifyUpdatedChartSpec(_.modified(lineIndex, _.toggleInverted)),
              )
            ),
            <.td(
              <.input(
                ^.tpe := "checkbox",
                ^.checked := line.cumulative,
                ^.onChange --> props.notifyUpdatedChartSpec(_.modified(lineIndex, _.toggleCumulative)),
              )
            ),
            <.td(
              Bootstrap.Button(Variant.info, Size.xs, tag = <.a)(
                Bootstrap.FontAwesomeIcon("times"),
                ^.onClick --> props.notifyUpdatedChartSpec(_.withRemovedLine(lineIndex)),
              )
            ),
          )
        )
      }
    }

    private def addButton(implicit props: Props): VdomNode = {
      Bootstrap.Button(Variant.info, tag = <.a)(
        Bootstrap.FontAwesomeIcon("plus"),
        " ",
        i18n("app.add-line"),
        ^.onClick --> props.notifyUpdatedChartSpec(_.withAddedEmptyLine),
      )
    }
  }

  private object SingleTextInputForm extends HydroReactComponent {
    // **************** API ****************//
    def apply(
        defaultValue: String,
        onChange: String => Callback,
    ): VdomElement = {
      component(Props(defaultValue, onChange))
    }

    // **************** Implementation of HydroReactComponent methods ****************//
    override protected val config =
      ComponentConfig(
        backendConstructor = new Backend(_),
        initialStateFromProps = props => State(value = props.defaultValue),
      )

    // **************** Implementation of HydroReactComponent types ****************//
    protected case class Props(
        defaultValue: String,
        onChange: String => Callback,
    )
    protected case class State(value: String)

    protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
      override def render(props: Props, state: State): VdomElement = logExceptions {
        <.form(
          <.input(
            ^.tpe := "text",
            ^.value := state.value,
            ^^.ifThen(state.value != props.defaultValue) {
              ^.className := "value-has-changed"
            },
            ^.autoComplete := "off",
            ^.onChange ==> { (e: ReactEventFromInput) =>
              val newString = e.target.value
              $.modState(_.copy(value = newString))
            },
          ),
          " ",
          Bootstrap.Button(Variant.info, Size.xs, tpe = "submit")(
            ^.disabled := state.value == props.defaultValue,
            Bootstrap.FontAwesomeIcon("pencil"),
            ^.onClick ==> { (e: ReactEventFromInput) =>
              e.preventDefault()
              props.onChange(state.value)
            },
          ),
        )
      }
    }
  }
}
object ChartSpecInput {
  case class ChartSpec(
      lines: Seq[Line]
  ) {
    def withAddedEmptyLine: ChartSpec = {
      ChartSpec(lines :+ Line())
    }

    def withRemovedLine(index: Int): ChartSpec = {
      val mutableLines = lines.toBuffer
      mutableLines.remove(index)
      ChartSpec(mutableLines.toVector)
    }

    def modified(index: Int, modification: Line => Line) = {
      ChartSpec(lines.updated(index, modification(lines(index))))
    }

    def stringify: String = {
      lines.map(_.stringify).mkString(ChartSpec.lineDelimiter)
    }
  }
  object ChartSpec {
    val empty = ChartSpec(lines = Seq())

    private val lineDelimiter = "~~"

    def parseStringified(string: String): ChartSpec = {
      ChartSpec(string.split(lineDelimiter).filter(_.nonEmpty).map(Line.parseStringified).toVector)
    }
  }

  case class Line(
      query: String = "",
      inverted: Boolean = false,
      cumulative: Boolean = false,
  ) {
    def toggleInverted: Line = copy(inverted = !inverted)
    def toggleCumulative: Line = copy(cumulative = !cumulative)

    def stringify: String = {
      s"${if (inverted) "I" else "_"}${if (cumulative) "C" else "_"}$query"
    }
  }
  object Line {
    def parseStringified(string: String): Line = {
      Line(
        query = string.substring(2),
        inverted = string.charAt(0) match {
          case 'I' => true
          case '_' => false
        },
        cumulative = string.charAt(1) match {
          case 'C' => true
          case '_' => false
        },
      )
    }
  }
}

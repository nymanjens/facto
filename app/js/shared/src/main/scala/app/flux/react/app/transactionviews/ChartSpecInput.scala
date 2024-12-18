package app.flux.react.app.transactionviews

import app.common.money.CurrencyValueManager
import app.common.accounting.ChartSpec
import app.common.accounting.ChartSpec.AggregationPeriod
import app.common.accounting.ChartSpec.Line
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
import hydro.flux.react.ReactVdomUtils.<<
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

      val compressIcon = Bootstrap.FontAwesomeIcon("compress")

      <.span(
        Table(
          tableClasses = Seq("table-chart-spec"),
          tableHeaders = Seq(
            <.th(i18n("app.query"), ^.width := "100%"),
            <.th(i18n("app.inverted")),
            <.th(i18n("app.cumulative")),
            <.th(i18n("app.bars")),
            <.th(),
          ),
          tableRowDatas = tableRowDatas,
        ),
        <.div(
          toggleButton(
            props.chartSpec.correctForInflation,
            value => _.copy(correctForInflation = value),
            VdomArray(
              <.i(^.className := "icon-inflation"),
              " ",
              i18n("app.correct-for-inflation"),
            ),
          ),
          " ",
          dropDownButton[AggregationPeriod](
            optionLabels = Map(
              AggregationPeriod.Month -> VdomArray(compressIcon, " ", i18n("app.aggregate-by-month")),
              AggregationPeriod.Year -> VdomArray(compressIcon, " ", i18n("app.aggregate-by-year")),
            ),
            currentValue = props.chartSpec.aggregationPeriod,
            specUpdate = value => _.copy(aggregationPeriod = value),
          ),
        ),
        <.div(
          ^.marginTop := "5px",
          addButton,
          " ",
          clearButton,
        ),
      )
    }

    private def toggleButton(
        enabled: Boolean,
        specUpdate: Boolean => ChartSpec => ChartSpec,
        content: VdomNode,
    )(implicit props: Props): VdomNode = {
      def updateCallback = props.notifyUpdatedChartSpec(specUpdate(!enabled))
      Bootstrap.Button(if (enabled) Variant.primary else Variant.default)(
        content,
        " ",
        <.input(
          ^.tpe := "checkbox",
          ^.checked := enabled,
          ^.onChange --> updateCallback,
        ),
        ^.onClick --> updateCallback,
      )
    }

    private def dropDownButton[V](
        optionLabels: Map[V, VdomNode],
        currentValue: V,
        specUpdate: V => ChartSpec => ChartSpec,
    )(implicit props: Props): VdomNode = {
      Bootstrap.ButtonGroup(
        Bootstrap.Button(variant = Variant.default)(
          ^.className := "dropdown-toggle",
          VdomAttr("data-toggle") := "dropdown",
          VdomAttr("aria-haspopup") := "true",
          VdomAttr("aria-expanded") := "false",
          optionLabels(currentValue),
          " ",
          Bootstrap.FontAwesomeIcon("caret-down"),
        ),
        <.ul(
          ^.className := "dropdown-menu",
          (
            for ((value, label) <- optionLabels) yield {
              <.li(
                <.a(
                  label,
                  ^.onClick --> props.notifyUpdatedChartSpec(specUpdate(value)),
                )
              )
            }
          ).toVdomArray,
        ),
      )
    }

    private def tableRowDatas(implicit props: Props): Seq[Table.TableRowData] = {
      for ((line, lineIndex) <- props.chartSpec.lines.zipWithIndex) yield {
        Table.TableRowData(
          Seq[VdomElement](
            <.td(
              SingleTextInputForm(
                defaultValue = line.query,
                onChange = newQuery =>
                  props.notifyUpdatedChartSpec(
                    modifyAndCopyNameFromQuery(lineIndex, _.copy(query = newQuery))
                  ),
              )
            ),
            <.td(
              <.input(
                ^.tpe := "checkbox",
                ^.checked := line.inverted,
                ^.onChange --> props.notifyUpdatedChartSpec(
                  modifyAndCopyNameFromQuery(lineIndex, _.toggleInverted)
                ),
              )
            ),
            <.td(
              <.input(
                ^.tpe := "checkbox",
                ^.checked := line.cumulative,
                ^.onChange --> props.notifyUpdatedChartSpec(
                  modifyAndCopyNameFromQuery(lineIndex, _.toggleCumulative)
                ),
              )
            ),
            <.td(
              <.input(
                ^.tpe := "checkbox",
                ^.checked := line.showBars,
                ^.onChange --> props.notifyUpdatedChartSpec(
                  modifyAndCopyNameFromQuery(lineIndex, _.toggleShowBars)
                ),
              )
            ),
            <.td(
              <<.ifThen(props.chartSpec.lines.size > 1 || lineIndex > 0) {
                Bootstrap.Button(Variant.info, Size.xs, tag = <.a)(
                  Bootstrap.FontAwesomeIcon("times"),
                  ^.onClick --> props.notifyUpdatedChartSpec(_.withRemovedLine(lineIndex)),
                )
              }
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

    private def clearButton(implicit props: Props): VdomNode = {
      Bootstrap.Button(Variant.default, tag = <.a)(
        Bootstrap.FontAwesomeIcon("times"),
        " ",
        i18n("app.clear"),
        ^.onClick --> props.notifyUpdatedChartSpec(_ => ChartSpec.singleEmptyLine()),
      )
    }

    private def modifyAndCopyNameFromQuery(lineIndex: Int, lineModification: Line => Line)(
        chartSpec: ChartSpec
    ): ChartSpec = {
      chartSpec.modified(
        lineIndex,
        lineModification andThen { newLine =>
          newLine.copy(name = s"${i18n("app.line-n", lineIndex + 1)}: '${newLine.query}'")
        },
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

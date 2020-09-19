package hydro.jsfacades

import japgolly.scalajs.react.Children
import japgolly.scalajs.react.JsComponent
import japgolly.scalajs.react.vdom.html_<^.VdomNode

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSImport

object Recharts {

  // **************** API ****************//
  def ResponsiveContainer(
      width: String,
      height: Int,
  )(children: VdomNode*) = {
    val component = JsComponent[js.Object, Children.Varargs, Null](ResponsiveContainerComponent)
    component(
      js.Dynamic.literal(
        width = width,
        height = height,
      )
    )(children: _*)
  }

  def LineChart(
      data: Seq[Map[String, js.Any]],
      margin: Margin,
  )(children: VdomNode*) = {
    val component = JsComponent[js.Object, Children.Varargs, Null](LineChartComponent)
    component(
      js.Dynamic.literal(
        data = data.map(_.toJSDictionary).toJSArray,
        margin = margin.toJsObject,
      )
    )(children: _*)
  }

  def Line(
      key: String,
      tpe: String,
      dataKey: String,
      stroke: String,
  ) = {
    val component = JsComponent[js.Object, Children.None, Null](LineComponent)
    component
      .withKey(key)
      .apply(
        js.Dynamic.literal(
          `type` = tpe,
          dataKey = dataKey,
          stroke = stroke,
          isAnimationActive = false,
        )
      )
  }

  def CartesianGrid(strokeDasharray: String, vertical: Boolean) = {
    val component = JsComponent[js.Object, Children.None, Null](CartesianGridComponent)
    component(
      js.Dynamic.literal(
        strokeDasharray = strokeDasharray,
        vertical = vertical,
      )
    )
  }

  def XAxis(dataKey: String) = {
    val component = JsComponent[js.Object, Children.None, Null](XAxisComponent)
    component(
      js.Dynamic.literal(
        dataKey = dataKey
      )
    )
  }

  def YAxis() = {
    val component = JsComponent[js.Object, Children.None, Null](YAxisComponent)
    component(js.Dynamic.literal())
  }

  def Tooltip() = {
    val component = JsComponent[js.Object, Children.None, Null](TooltipComponent)
    component(js.Dynamic.literal())
  }

  def Legend() = {
    val component = JsComponent[js.Object, Children.None, Null](LegendComponent)
    component(js.Dynamic.literal())
  }

  // **************** Public inner types ****************//
  case class Margin(
      top: Int,
      right: Int,
      left: Int,
      bottom: Int,
  ) {
    def toJsObject: js.Object = js.Dynamic.literal(
      top = top,
      right = right,
      left = left,
      bottom = bottom,
    )
  }

  // **************** Private inner types ****************//
  @JSImport("recharts", "ResponsiveContainer")
  @js.native
  private object ResponsiveContainerComponent extends js.Object

  @JSImport("recharts", "LineChart")
  @js.native
  private object LineChartComponent extends js.Object

  @JSImport("recharts", "Line")
  @js.native
  private object LineComponent extends js.Object

  @JSImport("recharts", "CartesianGrid")
  @js.native
  private object CartesianGridComponent extends js.Object

  @JSImport("recharts", "XAxis")
  @js.native
  private object XAxisComponent extends js.Object

  @JSImport("recharts", "YAxis")
  @js.native
  private object YAxisComponent extends js.Object

  @JSImport("recharts", "Tooltip")
  @js.native
  private object TooltipComponent extends js.Object

  @JSImport("recharts", "Legend")
  @js.native
  private object LegendComponent extends js.Object
}

package flux.react.uielements

import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object Panel {
  private case class Props(title: String, panelClasses: Seq[String], widthInColumns: Int)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC((_, props, children) =>
      <.div(
        ^^.classes("row" +: props.panelClasses),
        <.div(
          ^^.classes(s"col-lg-${props.widthInColumns}"),
          <.div(
            ^^.classes("panel panel-default"),
            <.div(^^.classes("panel-heading"), props.title),
            <.div(^^.classes("panel-body"), children))
        )
    ))
    .build

  def apply(title: String, panelClasses: Seq[String] = Seq(), key: String = null, widthInColumns: Int = 12)(
      children: VdomNode*): VdomElement = {
    val props = Props(title, panelClasses, widthInColumns)
    if (key == null) {
      component(props)(children: _*)
    } else {
      component.withKey(key).apply(props)(children: _*)
    }
  }
}

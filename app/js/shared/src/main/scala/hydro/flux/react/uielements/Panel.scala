package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object Panel {
  private case class Props(title: String, panelClasses: Seq[String])
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC(
      (_, props, children) =>
        Bootstrap.Row(
          ^^.classes(props.panelClasses),
          Bootstrap.Col(lg = 12)(
            Bootstrap.Panel()(
              Bootstrap.PanelHeading(props.title),
              Bootstrap.PanelBody(children),
            ))
      ))
    .build

  def apply(title: String, panelClasses: Seq[String] = Seq(), key: String = null)(
      children: VdomNode*): VdomElement = {
    if (key == null) {
      component(Props(title, panelClasses))(children: _*)
    } else {
      component.withKey(key).apply(Props(title, panelClasses))(children: _*)
    }
  }
}

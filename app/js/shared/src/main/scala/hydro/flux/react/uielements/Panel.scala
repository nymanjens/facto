package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object Panel {
  def apply(
      title: String,
      panelClasses: Seq[String] = Seq(),
      key: String = null,
      lg: Int = 12,
  )(
      children: VdomNode*
  ): VdomElement = {
    Bootstrap.Row(
      ^^.classes(panelClasses),
      ^^.ifThen(key != null) { ^.key := key },
      Bootstrap.Col(lg = lg)(
        Bootstrap.Panel()(
          Bootstrap.PanelHeading(title),
          Bootstrap.PanelBody(children: _*),
        )
      ),
    )
  }
}

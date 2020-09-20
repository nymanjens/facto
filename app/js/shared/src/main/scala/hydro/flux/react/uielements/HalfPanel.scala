package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object HalfPanel {
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC((_, props, children) =>
      Bootstrap.Col(lg = 6)(
        ^^.classes(props.panelClasses),
        Bootstrap.Panel()(
          Bootstrap.PanelHeading(
            props.title,
            <<.ifThen(props.closeButtonCallback.isDefined) {
              <.div(
                ^.className := "pull-right",
                Bootstrap.Button(variant = Variant.default, size = Size.xs)(
                  ^.onClick --> props.closeButtonCallback.get,
                  Bootstrap.FontAwesomeIcon("times", fixedWidth = true),
                ),
              )
            },
          ),
          Bootstrap.PanelBody(children),
        ),
      )
    )
    .build

  // **************** API ****************//
  def apply(
      title: VdomElement,
      panelClasses: Seq[String] = Seq(),
      closeButtonCallback: Option[Callback] = None,
  )(children: VdomNode*): VdomElement = {
    component(Props(title, panelClasses, closeButtonCallback))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(
      title: VdomElement,
      panelClasses: Seq[String],
      closeButtonCallback: Option[Callback],
  )
}

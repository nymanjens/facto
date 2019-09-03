package app.flux.react.uielements

import hydro.common.I18n
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object CollapseAllExpandAllButtons {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      <.span(
        Bootstrap.Button(variant = Variant.default)(
          ^.onClick --> Callback(props.onExpandedUpdate(false)),
          Bootstrap.FontAwesomeIcon("angle-double-right"),
          " ",
          props.i18n("app.collapse-all")
        ),
        " ",
        Bootstrap.Button(variant = Variant.default)(
          ^.onClick --> Callback(props.onExpandedUpdate(true)),
          Bootstrap.FontAwesomeIcon("angle-double-down"),
          " ",
          props.i18n("app.expand-all")
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(onExpandedUpdate: Boolean => Unit)(implicit i18n: I18n): VdomElement = {
    component(Props(onExpandedUpdate = onExpandedUpdate))
  }

  // **************** Private inner types ****************//
  private case class Props(onExpandedUpdate: Boolean => Unit)(implicit val i18n: I18n)
}

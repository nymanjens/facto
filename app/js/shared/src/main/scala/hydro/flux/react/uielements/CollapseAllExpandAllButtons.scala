package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.common.Unique
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object CollapseAllExpandAllButtons {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      <.span(
        Bootstrap.Button(variant = Variant.default)(
          ^.onClick --> Callback(props.setExpandedCallback(Unique(false)).runNow()),
          Bootstrap.FontAwesomeIcon("angle-double-right"),
          " ",
          props.i18n("app.collapse-all")
        ),
        " ",
        Bootstrap.Button(variant = Variant.default)(
          ^.onClick --> Callback(props.setExpandedCallback(Unique(true)).runNow()),
          Bootstrap.FontAwesomeIcon("angle-double-down"),
          " ",
          props.i18n("app.expand-all")
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(setExpandedCallback: Unique[Boolean] => Callback)(implicit i18n: I18n): VdomElement = {
    component(Props(setExpandedCallback = setExpandedCallback))
  }

  // **************** Private inner types ****************//
  private case class Props(setExpandedCallback: Unique[Boolean] => Callback)(implicit val i18n: I18n)
}

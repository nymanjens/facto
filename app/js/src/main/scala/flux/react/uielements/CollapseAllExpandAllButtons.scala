package flux.react.uielements

import common.{I18n, Unique}
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object CollapseAllExpandAllButtons {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      <.span(
        <.div(
          ^^.classes("btn", "btn-default"),
          ^.role := "button",
          ^.onClick --> Callback(props.setExpandedCallback(Unique(false)).runNow()),
          <.i(^^.classes("fa", "fa-minus", "fa-fw")),
          " ",
          props.i18n("facto.collapse-all")
        ),
        " ",
        <.div(
          ^^.classes("btn", "btn-default"),
          ^.role := "button",
          ^.onClick --> Callback(props.setExpandedCallback(Unique(true)).runNow()),
          <.i(^^.classes("fa", "fa-plus", "fa-fw")),
          " ",
          props.i18n("facto.expand-all")
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

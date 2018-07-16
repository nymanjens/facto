package flux.react.uielements

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object TransactionGroupEditButton {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      router.anchorWithHrefTo(Page.EditTransactionGroup(props.groupId))(
        ^^.classes("btn", "btn-default", "btn-xs"),
        ^.role := "button",
        <.i(^^.classes("fa", "fa-pencil", "fa-fw")),
        " ",
        props.i18n("app.edit")
      )
    })
    .build

  // **************** API ****************//
  def apply(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomElement = {
    component(Props(groupId))
  }

  // **************** Private inner types ****************//
  private case class Props(groupId: Long)(implicit val router: RouterContext, val i18n: I18n)
}

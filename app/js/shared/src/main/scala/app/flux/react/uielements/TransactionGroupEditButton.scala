package app.flux.react.uielements

import app.common.I18n
import app.flux.router.AppPages
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object TransactionGroupEditButton {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      router.anchorWithHrefTo(AppPages.EditTransactionGroup(props.groupId))(
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

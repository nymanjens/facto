package flux.react.uielements

import common.I18n
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.router.Page
import flux.stores.entries.GroupedTransactions
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._

object TransactionGroupEditButton {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      <.a(
        ^^.classes("btn", "btn-default", "btn-xs"),
        ^.href := props.router.pathFor(Page.EditTransactionGroupPage(props.groupId)).value,
        ^.role := "button",
        <.i(^^.classes("fa", "fa-pencil", "fa-fw")),
        props.i18n("facto.edit")
      )
    })
    .build

  // **************** API ****************//
  def apply(groupId: Long, router: RouterCtl[Page])(implicit i18n: I18n): VdomElement = {
    component(Props(groupId, router))
  }

  // **************** Private inner types ****************//
  private case class Props(groupId: Long, router: RouterCtl[Page])(implicit val i18n: I18n)
}

package flux.react.uielements

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.stores.entries.GroupedTransactions
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object TransactionGroupEditButton {
  private case class Props(groupId: Long, i18n: I18n)
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      <.a(^^.classes("btn", "btn-default", "btn-xs"),
        ^.href := "#",
        ^.role := "button",
        <.i(^^.classes("fa", "fa-pencil", "fa-fw")),
        props.i18n("facto.edit")
      )
    }
    ).build

  def apply(groupId: Long)(implicit i18n: I18n): ReactElement = {
    component(Props(groupId, i18n))
  }
}

package flux.react.app.transactiongroupform

import common.I18n
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import models.{EntityAccess, User}
import models.accounting.config.Config

import scala.collection.immutable.Seq

private[transactiongroupform] final class AddTransactionPanel(implicit i18n: I18n) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      HalfPanel(
        title = <.span(i18n("facto.new-transaction")),
        panelClasses = Seq("add-transaction-button-holder"))(
        <.table(
          <.tbody(
            <.tr(
              <.td(
                <.button(
                  ^.tpe := "button",
                  ^.className := "btn btn-primary btn-huge add-transaction-button",
                  ^.onClick --> props.onClick,
                  <.span(
                    ^.className := "glyphicon glyphicon-plus",
                    ^.aria.hidden := true
                  )
                )
              )
            )
          )
        )
      )
    ).build

  // **************** API ****************//
  def apply(onClick: Callback): ReactElement = {
    component(Props(onClick))
  }

  // **************** Private inner types ****************//
  private case class Props(onClick: Callback)
}

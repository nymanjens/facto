package flux.react.app.transactiongroupform

import common.I18n
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^

import scala.collection.immutable.Seq

object AddTransactionPanel {
  private case class Props(onClick: Callback, i18n: I18n)
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      HalfPanel(
        title = <.span(props.i18n("facto.new-transaction")),
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

  def apply(onClick: Callback)(implicit i18n: I18n): ReactElement = {
    component(Props(onClick, i18n))
  }
}

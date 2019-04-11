package app.flux.react.app.transactiongroupform

import hydro.common.I18n
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.HalfPanel
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[transactiongroupform] final class AddTransactionPanel(implicit i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      logExceptions {
        HalfPanel(
          title = <.span(i18n("app.new-transaction")),
          panelClasses = Seq("add-transaction-button-holder"))(
          <.table(
            <.tbody(
              <.tr(
                <.td(
                  Bootstrap.Button(Variant.primary)(
                    ^.className := "btn-huge add-transaction-button",
                    ^.onClick --> props.onClick,
                    Bootstrap.Glyphicon("plus")(
                      ^.aria.hidden := true
                    ),
                  )
                )
              )
            )
          )
        )
    })
    .build

  // **************** API ****************//
  def apply(onClick: Callback): VdomElement = {
    component(Props(onClick))
  }

  // **************** Private inner types ****************//
  private case class Props(onClick: Callback)
}

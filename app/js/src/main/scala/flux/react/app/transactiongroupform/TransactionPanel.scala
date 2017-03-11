package flux.react.app.transactiongroupform

import common.{I18n, LoggingUtils}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^
import flux.react.uielements
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

object TransactionPanel {
  private case class Props(title: String, deleteButtonCallback: Option[Callback], i18n: I18n)

  private val priceRef = uielements.bootstrap.TextInput.ref("price")

  private class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props, state: Unit) = LoggingUtils.logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        uielements.bootstrap.TextInput("label", "price", ref = priceRef),
        <.button(
          ^.onClick --> Callback {
            priceRef($).setValue("test value")
            println("  " + priceRef($).value)
          },
          "Test button"
        )
      )
    }
  }
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  def apply(closeButtonCallback: Option[Callback] = None)(implicit i18n: I18n): ReactElement = {
    component(Props("testTitle", closeButtonCallback, i18n))
  }
}

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

  private val price1Ref = uielements.bootstrap.TextInput.ref("price1")
  private val price2Ref = uielements.bootstrap.TextInput.ref("price2")

  private class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props, state: Unit) = LoggingUtils.logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        uielements.bootstrap.TextInput("label", "price 1", ref = price1Ref),
        DefaultFromReference(
          inputElementRef = price2Ref,
          defaultValueRef = price1Ref)(extraProps =>
          uielements.bootstrap.TextInput(
            "label", "price 2", inputClasses = extraProps.inputClasses, ref = extraProps.ref)
        ),
        <.button(
          ^.onClick --> Callback {
            price1Ref($).setValue("test value")
            println("  Price 1:" + price1Ref($).value)
            println("  Price 2:" + price2Ref($).value)
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

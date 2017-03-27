package flux.react.app.transactiongroupform

import common.{I18n, LoggingUtils}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{^^, <<}
import flux.react.uielements
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[transactiongroupform] object TransactionPanel {

  private val price1Ref = uielements.bootstrap.TextInput.ref("price1")
  private val price2Ref = InputWithDefaultFromReference.ref("price2")
  private val price3Ref = InputWithDefaultFromReference.ref("price3")
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(key: Int,
            ref: Reference,
            title: String,
            defaultPanel: Option[ComponentProxy],
            closeButtonCallback: Option[Callback] = None)(implicit i18n: I18n): ReactElement = {
    val props = Props(
      title,
      defaultPanel = defaultPanel,
      closeButtonCallback,
      i18n)
    component.withKey(key).withRef(ref.refComp)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TransactionPanel](private[TransactionPanel] val refComp: RefComp[Props, State, Backend, _ <: TopNode]) {
    def apply($: BackendScope[_, _]): ComponentProxy = new ComponentProxy(() => refComp($).get)
  }

  final class ComponentProxy private[TransactionPanel](private val componentProvider: () => ReactComponentU[Props, State, Backend, _ <: TopNode]) {
    def price: uielements.bootstrap.TextInput.ComponentProxy = price2Ref(componentScope).input

    private def componentScope: BackendScope[Props, State] = componentProvider().backend.$
  }


  // **************** Private inner types ****************//
  private type State = Unit

  private case class Props(title: String,
                           defaultPanel: Option[ComponentProxy],
                           deleteButtonCallback: Option[Callback],
                           i18n: I18n)

  private class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        uielements.bootstrap.TextInput("price 1", ref = price1Ref),
        InputWithDefaultFromReference(
          ref = price2Ref,
          defaultValueProxy = price1Ref($)) {
          extraProps =>
            uielements.bootstrap.TextInput(
              "price 2", inputClasses = extraProps.inputClasses, ref = extraProps.ref)
        },
        <<.ifThen(props.defaultPanel) { panelProxy =>
          InputWithDefaultFromReference(
            ref = price3Ref,
            defaultValueProxy = panelProxy.price) {
            extraProps =>
              uielements.bootstrap.TextInput(
                "price 3", inputClasses = extraProps.inputClasses, ref = extraProps.ref)
          }
        },
        <.button(
          ^.onClick --> Callback {
            println("  Price 1:" + price1Ref($).value)
            println("  Price 2:" + price2Ref($).input.value)
            for (panel <- props.defaultPanel) {
              println("  Price 3:" + price3Ref($).input.value)
              println("  Panel.price:" + panel.price.value)
            }
          },
          "Test button"
        )
      )
    }
  }
}

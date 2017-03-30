package flux.react.app.transactiongroupform

import common.{I18n, LoggingUtils}
import japgolly.scalajs.react._
import flux.react.uielements.InputBase
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{^^, <<}
import flux.react.uielements
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[transactiongroupform] object TransactionPanel {

  private val price1Ref = InputWithDefaultFromReference.ref("price1")
  private val price2Ref = InputWithDefaultFromReference.ref("price2")
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(key: Int,
            ref: Reference,
            title: String,
            defaultPanel: Option[Proxy],
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
    def apply($: BackendScope[_, _]): Proxy = new Proxy(() => refComp($).get)
  }

  final class Proxy private[TransactionPanel](private val componentProvider: () => ReactComponentU[Props, State, Backend, _ <: TopNode]) {
    def price1: InputBase.Proxy = price1Ref(componentScope)
    def price2: InputBase.Proxy = price2Ref(componentScope)

    private def componentScope: BackendScope[Props, State] = componentProvider().backend.$
  }


  // **************** Private inner types ****************//
  private type State = Unit

  private case class Props(title: String,
                           defaultPanel: Option[Proxy],
                           deleteButtonCallback: Option[Callback],
                           i18n: I18n)

  private class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        InputWithDefaultFromReference(
          ref = price1Ref,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.price1),
          nameToDelegateRef = uielements.bootstrap.TextInput.ref(_)) {
          extraProps =>
            uielements.bootstrap.TextInput(
              ref = extraProps.ref, label = "price 2", inputClasses = extraProps.inputClasses)
        },
        InputWithDefaultFromReference(
          ref = price2Ref,
          defaultValueProxy = price1Ref($),
          nameToDelegateRef = InputWithDefaultFromReference.ref(_)) {
          extraProps1 =>
            InputWithDefaultFromReference(
              ref = extraProps1.ref,
              defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.price2),
              nameToDelegateRef = uielements.bootstrap.TextInput.ref(_)) {
              extraProps2 =>
                uielements.bootstrap.TextInput(
                  ref = extraProps2.ref, label = "price 3", inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses)
            }
        },
        <.button(
          ^.onClick --> Callback {
            println("  Price 1:" + price1Ref($).value)
            println("  Price 2:" + price2Ref($).value)
          },
          "Test button"
        )
      )
    }
  }
}

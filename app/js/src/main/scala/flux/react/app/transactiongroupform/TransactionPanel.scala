package flux.react.app.transactiongroupform

import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import common.{I18n, LoggingUtils}
import common.CollectionUtils.toListMap
import japgolly.scalajs.react._
import flux.react.uielements.InputBase
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.uielements
import models.{EntityAccess, User}
import models.accounting.config.{Account, Config}
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[transactiongroupform] final class TransactionPanel(implicit i18n: I18n,
                                                           accountingConfig: Config,
                                                           user: User,
                                                           entityAccess: EntityAccess) {

  private val price1Ref = InputWithDefaultFromReference.ref("price1")
  private val price2Ref = InputWithDefaultFromReference.ref("price2")
  private val beneficiaryAccountCodeRef = InputWithDefaultFromReference.ref("beneficiaryAccountCode")
  private val categoryCodeRef = InputWithDefaultFromReference.ref("categoryCode")

  private val component = {
    def calculateInitialState(props: Props): State = logExceptions {
      State(beneficiaryAccount = accountingConfig.personallySortedAccounts.head)
    }
    ReactComponentB[Props](getClass.getSimpleName)
      .initialState_P[State](calculateInitialState)
      .renderBackend[Backend]
      .build
  }

  // **************** API ****************//
  def apply(key: Int,
            ref: Reference,
            title: String,
            defaultPanel: Option[Proxy],
            closeButtonCallback: Option[Callback] = None): ReactElement = {
    val props = Props(
      title,
      defaultPanel = defaultPanel,
      closeButtonCallback
    )
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
    def beneficiaryAccountCode: InputBase.Proxy = beneficiaryAccountCodeRef(componentScope)
    def categoryCode: InputBase.Proxy = categoryCodeRef(componentScope)

    private def componentScope: BackendScope[Props, State] = componentProvider().backend.$
  }


  // **************** Private inner types ****************//
  private case class State(beneficiaryAccount: Account)

  private case class Props(title: String,
                           defaultPanel: Option[Proxy],
                           deleteButtonCallback: Option[Callback])

  private class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        InputWithDefaultFromReference(
          ref = price1Ref,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.price1),
          nameToDelegateRef = uielements.bootstrap.TextInput.ref(_)) {
          extraProps =>
            uielements.bootstrap.TextInput(
              ref = extraProps.ref, label = "price 1", inputClasses = extraProps.inputClasses)
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
                  ref = extraProps2.ref, label = "price 2", inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses)
            }
        },
        InputWithDefaultFromReference(
          ref = beneficiaryAccountCodeRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.beneficiaryAccountCode),
          nameToDelegateRef = uielements.bootstrap.SelectInput.ref(_)) {
          extraProps =>
            uielements.bootstrap.SelectInput(
              ref = extraProps.ref,
              label = i18n("facto.beneficiary"),
              inputClasses = extraProps.inputClasses,
              optionValueToName = toListMap(
                accountingConfig.personallySortedAccounts.map(acc => (acc.code, acc.longName))),
              listener = BeneficiaryAccountListener)
        },
        InputWithDefaultFromReference(
          ref = categoryCodeRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.categoryCode),
          nameToDelegateRef = uielements.bootstrap.SelectInput.ref(_)) {
          extraProps =>
            uielements.bootstrap.SelectInput(
              ref = extraProps.ref,
              label = i18n("facto.category"),
              inputClasses = extraProps.inputClasses,
              optionValueToName = toListMap(
                state.beneficiaryAccount.categories
                  .map(category => {
                    val name = if (category.helpText.isEmpty) category.name else s"${category.name} (${category.helpText})"
                    (category.code, name)
                  })))
        },
        <.button(
          ^.onClick --> LogExceptionsCallback {
            println("  Price 1:" + price1Ref($).value)
            println("  Price 2:" + price2Ref($).value)
            println("  BeneficiaryAccountCode:" + beneficiaryAccountCodeRef($).value)
            println("  CategoryCode:" + categoryCodeRef($).value)
          },
          "Test button"
        )
      )
    }

    private object BeneficiaryAccountListener extends InputBase.Listener {
      override def onChange(newValue: String, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(beneficiaryAccount = accountingConfig.accounts(newValue))).runNow()
      }
    }
  }
}

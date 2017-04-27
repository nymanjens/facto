package flux.react.app.transactiongroupform

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.{I18n, LoggingUtils}
import common.CollectionUtils.toListMap
import japgolly.scalajs.react._
import flux.react.uielements.InputBase
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.uielements
import flux.react.uielements.bootstrap.SelectInput
import models.{EntityAccess, User}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{Currency, MoneyWithGeneralCurrency}
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[transactiongroupform] final class TransactionPanel(implicit i18n: I18n,
                                                           accountingConfig: Config,
                                                           user: User,
                                                           entityAccess: EntityAccess) {

  private val moneyInputWithDefault = InputWithDefaultFromReference.forType[Long]
  private val reservoirInputWithDefault = InputWithDefaultFromReference.forType[MoneyReservoir]
  private val accountInputWithDefault = InputWithDefaultFromReference.forType[Account]
  private val categoryInputWithDefault = InputWithDefaultFromReference.forType[Category]
  private val reservoirSelectInput = SelectInput.forType[MoneyReservoir]
  private val accountSelectInput = SelectInput.forType[Account]
  private val categorySelectInput = SelectInput.forType[Category]

  private val price1Ref = moneyInputWithDefault.ref("price1")
  private val price2Ref = moneyInputWithDefault.ref("price2")
  private val moneyReservoirRef = reservoirInputWithDefault.ref("moneyReservoir")
  private val beneficiaryAccountRef = accountInputWithDefault.ref("beneficiaryAccount")
  private val categoryRef = categoryInputWithDefault.ref("category")

  private val component = {
    def calculateInitialState(props: Props): State = logExceptions {
      State(
        beneficiaryAccount = accountingConfig.personallySortedAccounts.head,
        moneyReservoir = selectableReservoirs().head)
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

  // **************** Private methods ****************//
  private def selectableReservoirs(currentReservoir: MoneyReservoir = null): Seq[MoneyReservoir] = {
    accountingConfig.moneyReservoirs(includeNullReservoir = false, includeHidden = true)
      .filter(r => r == currentReservoir || !r.hidden) ++
      Seq(MoneyReservoir.NullMoneyReservoir)
  }

  // **************** Public inner types ****************//
  final class Reference private[TransactionPanel](private[TransactionPanel] val refComp: RefComp[Props, State, Backend, _ <: TopNode]) {
    def apply($: BackendScope[_, _]): Proxy = new Proxy(() => refComp($).get)
  }

  final class Proxy private[TransactionPanel](private val componentProvider: () => ReactComponentU[Props, State, Backend, _ <: TopNode]) {
    def price1: InputBase.Proxy[Long] = price1Ref(componentScope)
    def price2: InputBase.Proxy[Long] = price2Ref(componentScope)
    def beneficiaryAccountCode: InputBase.Proxy[Account] = beneficiaryAccountRef(componentScope)
    def moneyReservoirCode: InputBase.Proxy[MoneyReservoir] = moneyReservoirRef(componentScope)
    def categoryCode: InputBase.Proxy[Category] = categoryRef(componentScope)

    private def componentScope: BackendScope[Props, State] = componentProvider().backend.$
  }


  // **************** Private inner types ****************//
  private case class State(beneficiaryAccount: Account,
                           moneyReservoir: MoneyReservoir)

  private case class Props(title: String,
                           defaultPanel: Option[Proxy],
                           deleteButtonCallback: Option[Callback])

  private class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(
        moneyInputWithDefault.forOption(
          ref = price1Ref,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.price1),
          nameToDelegateRef = uielements.bootstrap.MoneyInput.ref(_)) {
          extraProps =>
            uielements.bootstrap.MoneyInput(
              ref = extraProps.ref,
              label = "price 1",
              inputClasses = extraProps.inputClasses,
              currency = state.moneyReservoir.currency
            )
        },
        moneyInputWithDefault(
          ref = price2Ref,
          defaultValueProxy = price1Ref($),
          nameToDelegateRef = moneyInputWithDefault.ref(_)) {
          extraProps1 =>
            moneyInputWithDefault.forOption(
              ref = extraProps1.ref,
              defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.price2),
              nameToDelegateRef = uielements.bootstrap.MoneyInput.ref(_)) {
              extraProps2 =>
                uielements.bootstrap.MoneyInput(
                  ref = extraProps2.ref,
                  label = "price 2",
                  inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses,
                  currency = state.moneyReservoir.currency
                )
            }
        },
        reservoirInputWithDefault.forOption(
          ref = moneyReservoirRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.moneyReservoirCode),
          nameToDelegateRef = reservoirSelectInput.ref(_)) {
          extraProps =>
            reservoirSelectInput(
              ref = extraProps.ref,
              label = i18n("facto.reservoir"),
              defaultValue = state.moneyReservoir,
              inputClasses = extraProps.inputClasses,
              options = selectableReservoirs(state.moneyReservoir),
              valueToId = _.code,
              valueToName = _.name,
              listener = MoneyReservoirListener
            )
        },
        accountInputWithDefault.forOption(
          ref = beneficiaryAccountRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.beneficiaryAccountCode),
          directUserChangeOnly = true,
          nameToDelegateRef = accountSelectInput.ref(_)) {
          extraProps =>
            accountSelectInput(
              ref = extraProps.ref,
              label = i18n("facto.beneficiary"),
              defaultValue = state.beneficiaryAccount,
              inputClasses = extraProps.inputClasses,
              options = accountingConfig.personallySortedAccounts,
              valueToId = _.code,
              valueToName = _.longName,
              listener = BeneficiaryAccountListener
            )
        },
        categoryInputWithDefault.forOption(
          ref = categoryRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.categoryCode),
          directUserChangeOnly = true,
          nameToDelegateRef = categorySelectInput.ref(_)) {
          extraProps =>
            categorySelectInput(
              ref = extraProps.ref,
              label = i18n("facto.category"),
              inputClasses = extraProps.inputClasses,
              options = state.beneficiaryAccount.categories,
              valueToId = _.code,
              valueToName = category => if (category.helpText.isEmpty) category.name else s"${category.name} (${category.helpText})"
            )
        },
        <.button(
          ^.onClick --> LogExceptionsCallback {
            println("  Price 1:" + price1Ref($).value)
            println("  Price 2:" + price2Ref($).value)
            println("  BeneficiaryAccountCode:" + beneficiaryAccountRef($).value)
            println("  CategoryCode:" + categoryRef($).value)
          },
          "Test button"
        )
      )
    }

    private object BeneficiaryAccountListener extends InputBase.Listener[Account] {
      override def onChange(newValue: Account, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(beneficiaryAccount = newValue)).runNow()
      }
    }

    private object MoneyReservoirListener extends InputBase.Listener[MoneyReservoir] {
      override def onChange(newReservoir: MoneyReservoir, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(moneyReservoir = newReservoir)).runNow()
      }
    }
  }
}

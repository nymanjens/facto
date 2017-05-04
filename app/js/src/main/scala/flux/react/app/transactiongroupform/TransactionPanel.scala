package flux.react.app.transactiongroupform

import java.time.LocalDate

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.{I18n, LoggingUtils}
import common.CollectionUtils.toListMap
import common.time.Clock
import japgolly.scalajs.react._
import flux.react.uielements.InputBase
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.uielements
import models.{EntityAccess, User}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{Currency, ExchangeRateManager, MoneyWithGeneralCurrency}
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[transactiongroupform] final class TransactionPanel(implicit i18n: I18n,
                                                           accountingConfig: Config,
                                                           user: User,
                                                           entityAccess: EntityAccess,
                                                           exchangeRateManager: ExchangeRateManager,
                                                           clock: Clock) {

  private val reservoirInputWithDefault = InputWithDefaultFromReference.forType[MoneyReservoir]
  private val accountInputWithDefault = InputWithDefaultFromReference.forType[Account]
  private val categoryInputWithDefault = InputWithDefaultFromReference.forType[Category]
  private val stringWithDefault = InputWithDefaultFromReference.forType[String]

  private val dateMappedInput = uielements.MappedInput.forTypes[String, LocalDate]

  private val reservoirSelectInput = uielements.bootstrap.SelectInput.forType[MoneyReservoir]
  private val accountSelectInput = uielements.bootstrap.SelectInput.forType[Account]
  private val categorySelectInput = uielements.bootstrap.SelectInput.forType[Category]

  private val transactionDateRef = dateMappedInput.ref("transactionDate")
  private val consumedDateRef = dateMappedInput.ref("consumedDate")
  private val rawTransactionDateRef = dateMappedInput.delegateRef(transactionDateRef)
  private val rawConsumedDateRef = dateMappedInput.delegateRef(consumedDateRef)
  private val moneyReservoirRef = reservoirInputWithDefault.ref("moneyReservoir")
  private val beneficiaryAccountRef = accountInputWithDefault.ref("beneficiaryAccount")
  private val categoryRef = categoryInputWithDefault.ref("category")
  private val descriptionRef = stringWithDefault.ref("description")
  private val flowRef = uielements.bootstrap.MoneyInput.ref("flow")

  private val component = {
    def calculateInitialState(props: Props): State = logExceptions {
      State(
        transactionDate = clock.now.toLocalDate,
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
    def rawTransactionDate: InputBase.Proxy[String] = rawTransactionDateRef(componentScope)
    def rawConsumedDate: InputBase.Proxy[String] = rawConsumedDateRef(componentScope)
    def transactionDate: InputBase.Proxy[LocalDate] = transactionDateRef(componentScope)
    def consumedDate: InputBase.Proxy[LocalDate] = consumedDateRef(componentScope)
    def beneficiaryAccountCode: InputBase.Proxy[Account] = beneficiaryAccountRef(componentScope)
    def moneyReservoirCode: InputBase.Proxy[MoneyReservoir] = moneyReservoirRef(componentScope)
    def categoryCode: InputBase.Proxy[Category] = categoryRef(componentScope)
    def description: InputBase.Proxy[String] = descriptionRef(componentScope)
    def flow: InputBase.Proxy[Long] = flowRef(componentScope)

    private def componentScope: BackendScope[Props, State] = componentProvider().backend.$
  }


  // **************** Private inner types ****************//
  private case class State(transactionDate: LocalDate,
                           beneficiaryAccount: Account,
                           moneyReservoir: MoneyReservoir)

  private case class Props(title: String,
                           defaultPanel: Option[Proxy],
                           deleteButtonCallback: Option[Callback])

  private class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      HalfPanel(
        title = <.span(props.title),
        closeButtonCallback = props.deleteButtonCallback)(

        dateMappedInput(
          ref = transactionDateRef,
          defaultValue = clock.now.toLocalDate,
          valueTransformer = uielements.MappedInput.ValueTransformer.StringToLocalDate,
          listener = TransactionDateListener,
          nameToDelegateRef = stringWithDefault.ref) {
          mappedExtraProps =>
            stringWithDefault.forOption(
              ref = mappedExtraProps.ref,
              defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawTransactionDate),
              nameToDelegateRef = uielements.bootstrap.TextInput.general.ref(_)) {
              extraProps =>
                uielements.bootstrap.TextInput.general(
                  ref = extraProps.ref,
                  label = i18n("facto.date-payed"),
                  defaultValue = mappedExtraProps.defaultValue,
                  inputClasses = extraProps.inputClasses
                )
            }
        },
        dateMappedInput(
          ref = consumedDateRef,
          defaultValue = clock.now.toLocalDate,
          valueTransformer = uielements.MappedInput.ValueTransformer.StringToLocalDate,
          nameToDelegateRef = stringWithDefault.ref) {
          mappedExtraProps =>
            stringWithDefault(
              ref = mappedExtraProps.ref,
              defaultValueProxy = rawTransactionDateRef($),
              nameToDelegateRef = stringWithDefault.ref) {
              extraProps1 =>
                stringWithDefault.forOption(
                  ref = extraProps1.ref,
                  defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawConsumedDate),
                  nameToDelegateRef = uielements.bootstrap.TextInput.general.ref(_)) {
                  extraProps2 =>
                    uielements.bootstrap.TextInput.general(
                      ref = extraProps2.ref,
                      label = i18n("facto.date-consumed"),
                      defaultValue = mappedExtraProps.defaultValue,
                      inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses
                    )
                }
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
        stringWithDefault.forOption(
          ref = descriptionRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.description),
          nameToDelegateRef = uielements.bootstrap.TextInput.general.ref(_)) {
          extraProps =>
            uielements.bootstrap.TextInput.general(
              ref = extraProps.ref,
              label = i18n("facto.description"),
              defaultValue = "",
              inputClasses = extraProps.inputClasses
            )
        },
        uielements.bootstrap.MoneyInput(
          ref = flowRef,
          label = i18n("facto.flow"),
          currency = state.moneyReservoir.currency,
          date = state.transactionDate
        ),
        <.button(
          ^.onClick --> LogExceptionsCallback {
            println("  Transaction date:" + transactionDateRef($).value)
            println("  Consumed date:" + consumedDateRef($).value)
            println("  BeneficiaryAccountCode:" + beneficiaryAccountRef($).value)
            println("  CategoryCode:" + categoryRef($).value)
            println("  Description:" + descriptionRef($).value)
            println("  Flow:" + flowRef($).value)
          },
          "Test button"
        )
      )
    }

    private object TransactionDateListener extends InputBase.Listener[LocalDate] {
      override def onChange(newValue: LocalDate, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(transactionDate = newValue)).runNow()
      }
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

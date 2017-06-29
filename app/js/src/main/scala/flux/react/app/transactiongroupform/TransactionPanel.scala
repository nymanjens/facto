package flux.react.app.transactiongroupform
import japgolly.scalajs.react.component.Scala.MutableRef
import java.util.NoSuchElementException

import japgolly.scalajs.react.component.Scala.{MountedImpure, MutableRef}
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.{I18n, LoggingUtils, SinglePendingTaskQueue}
import common.CollectionUtils.toListMap
import common.time.{Clock, LocalDateTime, LocalDateTimes}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import flux.react.uielements.{InputBase, InputWithDefaultFromReference}
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.uielements
import japgolly.scalajs.react.CtorType.Props
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.internal.Box
import models.accounting.{Tag, Transaction}
import models.{EntityAccess, User}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money._
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

private[transactiongroupform] final class TransactionPanel(implicit i18n: I18n,
                                                           accountingConfig: Config,
                                                           user: User,
                                                           entityAccess: EntityAccess,
                                                           exchangeRateManager: ExchangeRateManager,
                                                           clock: Clock) {

  private val anythingChangedQueue = SinglePendingTaskQueue.create()

  private val reservoirInputWithDefault = InputWithDefaultFromReference.forType[MoneyReservoir]
  private val accountInputWithDefault = InputWithDefaultFromReference.forType[Account]
  private val categoryInputWithDefault = InputWithDefaultFromReference.forType[Category]
  private val stringInputWithDefault = InputWithDefaultFromReference.forType[String]

  private val dateMappedInput = uielements.MappedInput.forTypes[String, LocalDateTime]
  private val tagsMappedInput = uielements.MappedInput.forTypes[String, Seq[Tag]]

  private val reservoirSelectInput = uielements.bootstrap.SelectInput.forType[MoneyReservoir]
  private val accountSelectInput = uielements.bootstrap.SelectInput.forType[Account]
  private val categorySelectInput = uielements.bootstrap.SelectInput.forType[Category]

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialStateFromProps[State](props =>
        logExceptions {
          State(
            transactionDate = props.defaultValues.map(_.transactionDate) getOrElse LocalDateTimes
              .toStartOfDay(clock.now),
            beneficiaryAccount = props.defaultValues
              .map(_.beneficiary) getOrElse accountingConfig.personallySortedAccounts.head,
            moneyReservoir = props.defaultValues.map(_.moneyReservoir) getOrElse selectableReservoirs().head
          )
      })
      .renderBackend[Backend]
      .build
  }

  // **************** API ****************//
  def apply(key: Int,
            ref: Reference,
            title: String,
            defaultValues: Option[Transaction],
            forceFlowValue: Option[ReferenceMoney] = None,
            showErrorMessages: Boolean,
            defaultPanel: Option[Proxy],
            focusOnMount: Boolean,
            closeButtonCallback: Option[Callback] = None,
            onFormChange: () => Unit): VdomElement = {

    val props = Props(
      title = title,
      defaultValues = defaultValues,
      forceFlowValue = forceFlowValue,
      showErrorMessages = showErrorMessages,
      defaultPanel = defaultPanel,
      focusOnMount = focusOnMount,
      deleteButtonCallback = closeButtonCallback,
      onFormChange = onFormChange
    )
    ref.mutableRef.component.withKey(key.toString).apply(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Private methods ****************//
  private def selectableReservoirs(currentReservoir: MoneyReservoir = null): Seq[MoneyReservoir] = {
    accountingConfig
      .moneyReservoirs(includeNullReservoir = false, includeHidden = true)
      .filter(r => r == currentReservoir || !r.hidden) ++
      Seq(MoneyReservoir.NullMoneyReservoir)
  }

  // **************** Public inner types ****************//
  final class Reference private[TransactionPanel] (
      private[TransactionPanel] val mutableRef: MutableRef[Props, State, Backend, ThisCtorSummoner#CT]) {
    def apply(): Proxy = new Proxy(Option(mutableRef.value).orNull)
  }

  final class Proxy private[TransactionPanel] (private val component: ThisComponentU) {
    def rawTransactionDate: InputBase.Proxy[String] = backend.rawTransactionDateRef()
    def rawConsumedDate: InputBase.Proxy[String] = backend.rawConsumedDateRef()
    def beneficiaryAccount: InputBase.Proxy[Account] = backend.beneficiaryAccountRef()
    def moneyReservoir: InputBase.Proxy[MoneyReservoir] = backend.moneyReservoirRef()
    def category: InputBase.Proxy[Category] = backend.categoryRef()
    def description: InputBase.Proxy[String] = backend.descriptionRef()
    def detailDescription: InputBase.Proxy[String] = backend.detailDescriptionRef()
    def rawTags: InputBase.Proxy[String] = backend.rawTagsRef()

    def flowValueOrDefault: DatedMoney =
      DatedMoney(
        cents = backend.flowRef().valueOrDefault,
        currency = backend.moneyReservoirRef().valueOrDefault.currency,
        date = backend.transactionDateRef().valueOrDefault)

    def data: Option[Data] =
      try {
        Some(
          Data(
            transactionDate = backend.transactionDateRef().value.get,
            consumedDate = backend.consumedDateRef().value.get,
            moneyReservoir = backend.moneyReservoirRef().value.get,
            beneficiaryAccount = backend.beneficiaryAccountRef().value.get,
            category = backend.categoryRef().value.get,
            description = backend.descriptionRef().value.get,
            flow = DatedMoney(
              cents = backend.flowRef().value.get,
              currency = backend.moneyReservoirRef().value.get.currency,
              date = backend.transactionDateRef().value.get),
            detailDescription = backend.detailDescriptionRef().value.get,
            tags = backend.tagsRef().value.get
          ))
      } catch {
        case e: NoSuchElementException => None
      }

    private def backend = component.backend
  }

  case class Data(transactionDate: LocalDateTime,
                  consumedDate: LocalDateTime,
                  moneyReservoir: MoneyReservoir,
                  beneficiaryAccount: Account,
                  category: Category,
                  description: String,
                  flow: DatedMoney,
                  detailDescription: String,
                  tags: Seq[Tag])

  // **************** Private inner types ****************//
  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisComponentU = MountedImpure[Props, State, Backend]
  private case class State(transactionDate: LocalDateTime,
                           beneficiaryAccount: Account,
                           moneyReservoir: MoneyReservoir)

  private case class Props(title: String,
                           defaultValues: Option[Transaction],
                           forceFlowValue: Option[ReferenceMoney],
                           showErrorMessages: Boolean,
                           defaultPanel: Option[Proxy],
                           focusOnMount: Boolean,
                           deleteButtonCallback: Option[Callback],
                           onFormChange: () => Unit)

  private class Backend(val $ : BackendScope[Props, State]) {

    val transactionDateRef = dateMappedInput.ref()
    val consumedDateRef = dateMappedInput.ref()
    val rawTransactionDateRef = dateMappedInput.delegateRef(transactionDateRef)
    val rawConsumedDateRef = dateMappedInput.delegateRef(consumedDateRef)
    val moneyReservoirRef = reservoirInputWithDefault.ref()
    val beneficiaryAccountRef = accountInputWithDefault.ref()
    val categoryRef = categoryInputWithDefault.ref()
    val descriptionRef = stringInputWithDefault.ref()
    val flowRef = uielements.bootstrap.MoneyInput.ref()
    val detailDescriptionRef = stringInputWithDefault.ref()
    val tagsRef = tagsMappedInput.ref()
    val rawTagsRef = tagsMappedInput.delegateRef(tagsRef)

    def render(props: Props, state: State) = logExceptions {
      HalfPanel(title = <.span(props.title), closeButtonCallback = props.deleteButtonCallback)(
        dateMappedInput(
          ref = transactionDateRef,
          defaultValue = state.transactionDate,
          valueTransformer = uielements.MappedInput.ValueTransformer.StringToLocalDateTime,
          listener = TransactionDateListener,
          delegateRefFactory = stringInputWithDefault.ref _
        ) { mappedExtraProps =>
          stringInputWithDefault.forOption(
            ref = mappedExtraProps.ref,
            defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawTransactionDate),
            startWithDefault = props.defaultValues.isEmpty,
            delegateRefFactory = uielements.bootstrap.TextInput.ref _
          ) { extraProps =>
            uielements.bootstrap.TextInput(
              ref = extraProps.ref,
              label = i18n("facto.date-payed"),
              defaultValue = mappedExtraProps.defaultValue,
              showErrorMessage = props.showErrorMessages,
              inputClasses = extraProps.inputClasses,
              focusOnMount = props.focusOnMount
            )
          }
        },
        dateMappedInput(
          ref = consumedDateRef,
          defaultValue = props.defaultValues.map(_.consumedDate) getOrElse LocalDateTimes.toStartOfDay(
            clock.now),
          valueTransformer = uielements.MappedInput.ValueTransformer.StringToLocalDateTime,
          delegateRefFactory = stringInputWithDefault.ref _,
          listener = AnythingChangedListener
        ) { mappedExtraProps =>
          stringInputWithDefault(
            ref = mappedExtraProps.ref,
            defaultValueProxy = rawTransactionDateRef(),
            delegateRefFactory = stringInputWithDefault.ref _) {
            extraProps1 =>
              stringInputWithDefault.forOption(
                ref = extraProps1.ref,
                defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawConsumedDate),
                startWithDefault = props.defaultValues.isEmpty,
                delegateRefFactory = uielements.bootstrap.TextInput.ref _
              ) { extraProps2 =>
                uielements.bootstrap.TextInput(
                  ref = extraProps2.ref,
                  label = i18n("facto.date-consumed"),
                  defaultValue = mappedExtraProps.defaultValue,
                  showErrorMessage = props.showErrorMessages,
                  inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses
                )
              }
          }
        },
        reservoirInputWithDefault.forOption(
          ref = moneyReservoirRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.moneyReservoir),
          startWithDefault = props.defaultValues.isEmpty,
          delegateRefFactory = reservoirSelectInput.ref _
        ) { extraProps =>
          reservoirSelectInput(
            ref = extraProps.ref,
            label = i18n("facto.payed-with-to"),
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
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.beneficiaryAccount),
          startWithDefault = props.defaultValues.isEmpty,
          directUserChangeOnly = true,
          delegateRefFactory = accountSelectInput.ref _
        ) { extraProps =>
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
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.category),
          startWithDefault = props.defaultValues.isEmpty,
          directUserChangeOnly = true,
          delegateRefFactory = categorySelectInput.ref _
        ) { extraProps =>
          categorySelectInput(
            ref = extraProps.ref,
            label = i18n("facto.category"),
            defaultValue = props.defaultValues
              .map(_.category) getOrElse state.beneficiaryAccount.categories.head,
            inputClasses = extraProps.inputClasses,
            options = state.beneficiaryAccount.categories,
            valueToId = _.code,
            valueToName = category =>
              if (category.helpText.isEmpty) category.name else s"${category.name} (${category.helpText})",
            listener = AnythingChangedListener
          )
        },
        stringInputWithDefault.forOption(
          ref = descriptionRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.description),
          startWithDefault = props.defaultValues.isEmpty,
          delegateRefFactory = uielements.bootstrap.TextInput.ref _
        ) { extraProps =>
          uielements.bootstrap.TextInput(
            ref = extraProps.ref,
            label = i18n("facto.description"),
            defaultValue = props.defaultValues.map(_.description) getOrElse "",
            required = true,
            showErrorMessage = props.showErrorMessages,
            inputClasses = extraProps.inputClasses,
            listener = AnythingChangedListener
          )
        },
        uielements.bootstrap.MoneyInput(
          ref = flowRef,
          label = i18n("facto.flow"),
          defaultValue = props.defaultValues.map(_.flowInCents) getOrElse 0,
          required = true,
          showErrorMessage = props.showErrorMessages,
          forceValue = props.forceFlowValue.map(
            _.withDate(state.transactionDate)
              .exchangedForCurrency(state.moneyReservoir.currency)
              .cents),
          currency = state.moneyReservoir.currency,
          date = state.transactionDate,
          listener = AnythingChangedListener
        ),
        stringInputWithDefault.forOption(
          ref = detailDescriptionRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.detailDescription),
          startWithDefault = props.defaultValues.isEmpty,
          delegateRefFactory = uielements.bootstrap.TextAreaInput.ref _
        ) { extraProps =>
          uielements.bootstrap.TextAreaInput(
            ref = extraProps.ref,
            label = i18n("facto.more-info"),
            defaultValue = props.defaultValues.map(_.detailDescription) getOrElse "",
            showErrorMessage = props.showErrorMessages,
            inputClasses = extraProps.inputClasses,
            listener = AnythingChangedListener
          )
        },
        tagsMappedInput(
          ref = tagsRef,
          defaultValue = props.defaultValues.map(_.tags) getOrElse Seq(),
          valueTransformer = uielements.MappedInput.ValueTransformer.StringToTags,
          delegateRefFactory = stringInputWithDefault.ref _,
          listener = AnythingChangedListener
        ) { mappedExtraProps =>
          stringInputWithDefault.forOption(
            ref = mappedExtraProps.ref,
            defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawTags),
            startWithDefault = props.defaultValues.isEmpty,
            delegateRefFactory = uielements.bootstrap.TextInput.ref _
          ) { extraProps =>
            uielements.bootstrap.TextInput(
              ref = extraProps.ref,
              label = i18n("facto.tags"),
              showErrorMessage = props.showErrorMessages,
              defaultValue = mappedExtraProps.defaultValue,
              inputClasses = extraProps.inputClasses
            )
          }
        }
      )
    }

    private object TransactionDateListener extends InputBase.Listener[LocalDateTime] {
      override def onChange(newValue: LocalDateTime, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(transactionDate = newValue)).runNow()
        AnythingChangedListener.onChange(newValue, directUserChange).runNow()
      }
    }
    private object BeneficiaryAccountListener extends InputBase.Listener[Account] {
      override def onChange(newValue: Account, directUserChange: Boolean) = LogExceptionsCallback {
        $.modState(_.copy(beneficiaryAccount = newValue)).runNow()
        AnythingChangedListener.onChange(newValue, directUserChange).runNow()
      }
    }
    private object MoneyReservoirListener extends InputBase.Listener[MoneyReservoir] {
      override def onChange(newReservoir: MoneyReservoir, directUserChange: Boolean) =
        LogExceptionsCallback {
          $.modState(_.copy(moneyReservoir = newReservoir)).runNow()
          AnythingChangedListener.onChange(newReservoir, directUserChange).runNow()
        }
    }
    private object AnythingChangedListener extends InputBase.Listener[Any] {
      override def onChange(newValue: Any, directUserChange: Boolean) = LogExceptionsCallback {
        // Schedule in future because querying the values of TransactionPanel.Proxy would yield
        // outdated values at the moment.
        anythingChangedQueue.execute {
          $.props.runNow().onFormChange()
        }
      }
    }
  }
}

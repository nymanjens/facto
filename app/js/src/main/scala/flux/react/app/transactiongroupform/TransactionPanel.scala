package flux.react.app.transactiongroupform
import java.util.NoSuchElementException

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.accounting.Tags
import common.time.{Clock, LocalDateTime, LocalDateTimes}
import common.{I18n, SinglePendingTaskQueue}
import flux.react.ReactVdomUtils.<<
import flux.react.uielements.HalfPanel
import flux.react.uielements.input.bootstrap.{MoneyInput, SelectInput, TextAreaInput, TextInput}
import flux.react.uielements.input.{InputBase, InputWithDefaultFromReference, MappedInput, bootstrap}
import flux.stores.entries.TagsStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.{MountedImpure, MutableRef}
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money._
import models.accounting.Transaction
import models.{EntityAccess, User}

import scala.collection.immutable.Seq

private[transactiongroupform] final class TransactionPanel(implicit i18n: I18n,
                                                           accountingConfig: Config,
                                                           user: User,
                                                           entityAccess: EntityAccess,
                                                           exchangeRateManager: ExchangeRateManager,
                                                           clock: Clock,
                                                           tagsStoreFactory: TagsStoreFactory) {

  private val anythingChangedQueue = SinglePendingTaskQueue.create()

  private val reservoirInputWithDefault = InputWithDefaultFromReference.forType[MoneyReservoir]
  private val accountInputWithDefault = InputWithDefaultFromReference.forType[Account]
  private val categoryInputWithDefault = InputWithDefaultFromReference.forType[Category]
  private val stringInputWithDefault = InputWithDefaultFromReference.forType[String]
  private val tagsInputWithDefault = InputWithDefaultFromReference.forType[Seq[String]]

  private val dateMappedInput = MappedInput.forTypes[String, LocalDateTime]

  private val reservoirSelectInput = SelectInput.forType[MoneyReservoir]
  private val accountSelectInput = bootstrap.SelectInput.forType[Account]
  private val categorySelectInput = bootstrap.SelectInput.forType[Category]

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialStateFromProps[State](props =>
        logExceptions {
          State(
            transactionDate = props.defaultValues.transactionDate getOrElse LocalDateTimes
              .toStartOfDay(clock.now),
            beneficiaryAccount = props.defaultValues.beneficiary getOrElse accountingConfig.personallySortedAccounts.head,
            moneyReservoir = props.defaultValues.moneyReservoir getOrElse selectableReservoirs().head
          )
      })
      .renderBackend[Backend]
      .build
  }

  // **************** API ****************//
  def apply(key: Int,
            ref: Reference,
            title: String,
            defaultValues: Transaction.Partial,
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
    def apply(): Proxy = new Proxy(() => Option(mutableRef.value))
  }

  final class Proxy private[TransactionPanel] (
      private val maybeComponentFactory: () => Option[ThisComponentU]) {
    def rawTransactionDate: InputBase.Proxy[String] = fromBackendOrNull(_.rawTransactionDateRef())
    def rawConsumedDate: InputBase.Proxy[String] = fromBackendOrNull(_.rawConsumedDateRef())
    def beneficiaryAccount: InputBase.Proxy[Account] = fromBackendOrNull(_.beneficiaryAccountRef())
    def moneyReservoir: InputBase.Proxy[MoneyReservoir] = fromBackendOrNull(_.moneyReservoirRef())
    def category: InputBase.Proxy[Category] = fromBackendOrNull(_.categoryRef())
    def description: InputBase.Proxy[String] = fromBackendOrNull(_.descriptionRef())
    def detailDescription: InputBase.Proxy[String] = fromBackendOrNull(_.detailDescriptionRef())
    def tags: InputBase.Proxy[Seq[String]] = fromBackendOrNull(_.tagsRef())

    def flowValueOrDefault: DatedMoney = maybeBackend match {
      case Some(backend) =>
        DatedMoney(
          cents = backend.flowRef().valueOrDefault,
          currency = backend.moneyReservoirRef().valueOrDefault.currency,
          date = backend.transactionDateRef().valueOrDefault)
      case None => DatedMoney(0, Currency.default, clock.now)

    }

    def data: Option[Data] = maybeBackend flatMap { backend =>
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
    }

    private def fromBackendOrNull[Value](
        backendToProxy: Backend => InputBase.Proxy[Value]): InputBase.Proxy[Value] = {
      maybeBackend map (backendToProxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
    private def maybeBackend: Option[Backend] = maybeComponentFactory() map (_.backend)
  }

  case class Data(transactionDate: LocalDateTime,
                  consumedDate: LocalDateTime,
                  moneyReservoir: MoneyReservoir,
                  beneficiaryAccount: Account,
                  category: Category,
                  description: String,
                  flow: DatedMoney,
                  detailDescription: String,
                  tags: Seq[String])

  // **************** Private inner types ****************//
  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisComponentU = MountedImpure[Props, State, Backend]
  private case class State(transactionDate: LocalDateTime,
                           beneficiaryAccount: Account,
                           moneyReservoir: MoneyReservoir)

  private case class Props(title: String,
                           defaultValues: Transaction.Partial,
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
    val issuerRef = bootstrap.TextInput.ref()
    val moneyReservoirRef = reservoirInputWithDefault.ref()
    val beneficiaryAccountRef = accountInputWithDefault.ref()
    val categoryRef = categoryInputWithDefault.ref()
    val descriptionRef = stringInputWithDefault.ref()
    val flowRef = MoneyInput.ref()
    val detailDescriptionRef = stringInputWithDefault.ref()
    val tagsRef = tagsInputWithDefault.ref()

    def render(props: Props, state: State) = logExceptions {
      HalfPanel(title = <.span(props.title), closeButtonCallback = props.deleteButtonCallback)(
        dateMappedInput(
          ref = transactionDateRef,
          defaultValue = state.transactionDate,
          valueTransformer = MappedInput.ValueTransformer.StringToLocalDateTime,
          listener = TransactionDateListener,
          delegateRefFactory = stringInputWithDefault.ref _
        ) { mappedExtraProps =>
          stringInputWithDefault.forOption(
            ref = mappedExtraProps.ref,
            defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.rawTransactionDate),
            startWithDefault = props.defaultValues.isEmpty,
            delegateRefFactory = TextInput.ref _
          ) {
            extraProps =>
              bootstrap.TextInput(
                ref = extraProps.ref,
                name = "transaction-date",
                label = i18n("facto.date-payed"),
                defaultValue = mappedExtraProps.defaultValue,
                required = true,
                showErrorMessage = props.showErrorMessages,
                additionalValidator = mappedExtraProps.additionalValidator,
                inputClasses = extraProps.inputClasses,
                focusOnMount = props.focusOnMount
              )
          }
        },
        dateMappedInput(
          ref = consumedDateRef,
          defaultValue = props.defaultValues.consumedDate getOrElse LocalDateTimes.toStartOfDay(clock.now),
          valueTransformer = MappedInput.ValueTransformer.StringToLocalDateTime,
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
                delegateRefFactory = bootstrap.TextInput.ref _
              ) { extraProps2 =>
                bootstrap.TextInput(
                  ref = extraProps2.ref,
                  name = "date-consumed",
                  label = i18n("facto.date-consumed"),
                  defaultValue = mappedExtraProps.defaultValue,
                  required = true,
                  showErrorMessage = props.showErrorMessages,
                  additionalValidator = mappedExtraProps.additionalValidator,
                  inputClasses = extraProps1.inputClasses ++ extraProps2.inputClasses
                )
              }
          }
        },
        <<.ifThen(props.defaultValues.issuer.isDefined && props.defaultValues.issuer.get != user) {
          bootstrap.TextInput(
            ref = issuerRef,
            name = "issuer",
            label = i18n("facto.issuer"),
            defaultValue = props.defaultValues.issuer.get.name,
            disabled = true
          )
        },
        reservoirInputWithDefault.forOption(
          ref = moneyReservoirRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.moneyReservoir),
          startWithDefault = props.defaultValues.isEmpty,
          delegateRefFactory = reservoirSelectInput.ref _
        ) { extraProps =>
          reservoirSelectInput(
            ref = extraProps.ref,
            name = "payed-with-to",
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
            name = "beneficiary",
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
            name = "category",
            label = i18n("facto.category"),
            defaultValue = props.defaultValues.category getOrElse state.beneficiaryAccount.categories.head,
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
          delegateRefFactory = bootstrap.TextInput.ref _
        ) { extraProps =>
          bootstrap.TextInput(
            ref = extraProps.ref,
            name = "description",
            label = i18n("facto.description"),
            defaultValue = props.defaultValues.description,
            required = true,
            showErrorMessage = props.showErrorMessages,
            inputClasses = extraProps.inputClasses,
            listener = AnythingChangedListener
          )
        },
        bootstrap.MoneyInput.withCurrencyConversion(
          ref = flowRef,
          name = "flow",
          label = i18n("facto.flow"),
          defaultValue = props.defaultValues.flowInCents,
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
          delegateRefFactory = TextAreaInput.ref _
        ) { extraProps =>
          bootstrap.TextAreaInput(
            ref = extraProps.ref,
            name = "more-info",
            label = i18n("facto.more-info"),
            defaultValue = props.defaultValues.detailDescription,
            showErrorMessage = props.showErrorMessages,
            inputClasses = extraProps.inputClasses,
            listener = AnythingChangedListener
          )
        },
        tagsInputWithDefault.forOption(
          ref = tagsRef,
          defaultValueProxy = props.defaultPanel.map(proxy => () => proxy.tags),
          startWithDefault = props.defaultValues.isEmpty,
          delegateRefFactory = bootstrap.TagInput.ref _
        ) { extraProps =>
          bootstrap.TagInput(
            ref = extraProps.ref,
            name = "tags",
            label = i18n("facto.tags"),
            suggestions =
              // Note: This is not strictly correct because we are using changing state from outside
              // this component without putting it into the state (and listening to the tagsStore for
              // changes. This is because being up-to-date is not really necessary but would introduce
              // a lot of code.
              tagsStoreFactory.get().state.tagToTransactionIds.keySet.toVector,
            showErrorMessage = props.showErrorMessages,
            additionalValidator = _.forall(Tags.isValidTag),
            defaultValue = props.defaultValues.tags,
            inputClasses = extraProps.inputClasses,
            listener = AnythingChangedListener
          )
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

package flux.react.uielements.input.bootstrap

import common.I18n
import common.time.LocalDateTime
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.input.InputBase
import flux.react.uielements.input.bootstrap.InputComponent.Props
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.money.{Currency, DatedMoney, ExchangeRateManager, Money}

import scala.collection.immutable.Seq

object MoneyInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    valueChangeForPropsChange = (newProps, oldValue) => {
      newProps.extra.forceValue match {
        case Some(forceValue) => forceValue
        case None => oldValue
      }
    },
    inputRenderer = (classes: Seq[String],
                     name: String,
                     valueString: String,
                     onChange: ReactEventFromInput => Callback,
                     extraProps: ExtraProps) => {
      def referenceMoney(currency: Currency, date: LocalDateTime) = {
        val datedMoney = {
          val cents = ValueTransformer.stringToValue(valueString, extraProps) getOrElse 0L
          DatedMoney(cents, currency, date)
        }
        datedMoney.exchangedForReferenceCurrency(extraProps.exchangeRateManager)
      }

      <.div(
        ^.className := "input-group",
        <.span(
          ^.className := "input-group-addon",
          <.i(^.className := extraProps.currency.iconClass)
        ),
        <.input(
          ^.tpe := "text",
          ^.autoComplete := "off",
          ^^.classes(classes),
          ^.name := name,
          ^.value := valueString,
          ^.disabled := extraProps.forceValue.isDefined,
          ^.onChange ==> onChange
        ),
        ^^.ifThen(extraProps.currency.isForeign && extraProps.dateForCurrencyConversion.isDefined) {
          <.span(
            ^.className := "input-group-addon",
            <.i(^.className := Currency.default.iconClass),
            " ",
            referenceMoney(extraProps.currency, extraProps.dateForCurrencyConversion.get).formatFloat
          )
        }
      )
    }
  )

  // **************** API ****************//
  def withCurrencyConversion(ref: Reference,
                             name: String,
                             label: String,
                             defaultValue: Long,
                             required: Boolean = false,
                             showErrorMessage: Boolean,
                             inputClasses: Seq[String] = Seq(),
                             forceValue: Option[Long] = None,
                             currency: Currency,
                             date: LocalDateTime,
                             listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance)(
      implicit exchangeRateManager: ExchangeRateManager,
      i18n: I18n): VdomElement = applyInternal(
    ref = ref,
    name = name,
    label = label,
    defaultValue = defaultValue,
    required = required,
    showErrorMessage = showErrorMessage,
    inputClasses = inputClasses,
    forceValue = forceValue,
    currency = currency,
    dateForCurrencyConversion = Some(date),
    listener = listener
  )

  def apply(ref: Reference,
            name: String,
            label: String,
            defaultValue: Long,
            required: Boolean = false,
            showErrorMessage: Boolean,
            inputClasses: Seq[String] = Seq(),
            forceValue: Option[Long] = None,
            currency: Currency,
            listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance)(
      implicit exchangeRateManager: ExchangeRateManager,
      i18n: I18n): VdomElement = applyInternal(
    ref = ref,
    name = name,
    label = label,
    defaultValue = defaultValue,
    required = required,
    showErrorMessage = showErrorMessage,
    inputClasses = inputClasses,
    forceValue = forceValue,
    currency = currency,
    dateForCurrencyConversion = None,
    listener = listener
  )

  // **************** Private helper methods ****************//
  private def applyInternal(ref: Reference,
                            name: String,
                            label: String,
                            defaultValue: Long,
                            required: Boolean,
                            showErrorMessage: Boolean,
                            inputClasses: Seq[String],
                            forceValue: Option[Long],
                            currency: Currency,
                            dateForCurrencyConversion: Option[LocalDateTime],
                            listener: InputBase.Listener[Value])(
      implicit exchangeRateManager: ExchangeRateManager,
      i18n: I18n): VdomElement = {
    val props = Props[Value, ExtraProps](
      label = label,
      name = name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps(
        forceValue = forceValue,
        currency = currency,
        dateForCurrencyConversion = dateForCurrencyConversion),
      valueTransformer = ValueTransformer
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput] (
      private[MoneyInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps(forceValue: Option[Long],
                        currency: Currency,
                        dateForCurrencyConversion: Option[LocalDateTime])(
      implicit val exchangeRateManager: ExchangeRateManager)

  // **************** Private inner types ****************//
  /** Number of cents. */
  type Value = Long

  private object ValueTransformer extends InputComponent.ValueTransformer[Value, ExtraProps] {
    override def stringToValue(string: String, extraProps: ExtraProps) = {
      Money.floatStringToCents(string)
    }

    override def valueToString(cents: Long, extraProps: ExtraProps) = {
      Money.centsToFloatString(cents)
    }

    override def isEmptyValue(value: Long) = value == 0
  }
}

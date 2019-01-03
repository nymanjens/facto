package app.flux.react.uielements.input.bootstrap

import app.common.GuavaReplacement.Splitter
import app.common.I18n
import app.common.ScalaUtils.visibleForTesting
import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.ExchangeRateManager
import app.common.money.Money
import hydro.common.time.LocalDateTime
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.bootstrap.InputComponent
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object MoneyInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    valueChangeForPropsChange = (newProps, oldValue) => {
      newProps.extra.forceValue match {
        case Some(forceValue) => forceValue
        case None             => oldValue
      }
    },
    inputRenderer = (classes: Seq[String],
                     name: String,
                     valueString: String,
                     onChange: String => Callback,
                     extraProps: ExtraProps) => {
      def referenceMoney(currency: Currency, date: LocalDateTime) = {
        val datedMoney = {
          val cents = ValueTransformer.stringToValue(valueString, extraProps) getOrElse 0L
          DatedMoney(cents, currency, date)
        }
        datedMoney.exchangedForReferenceCurrency(extraProps.exchangeRateManager)
      }
      def materializeInputIfValid(): Callback = {
        ValueTransformer.stringToValue(valueString, extraProps) match {
          case Some(cents) => onChange(ValueTransformer.valueToString(cents, extraProps))
          case None        => Callback.empty
        }
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
          ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value)),
          ^.onBlur ==> ((event: ReactEventFromInput) => materializeInputIfValid())
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

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput] (
      private[MoneyInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps(
      forceValue: Option[Long],
      currency: Currency,
      dateForCurrencyConversion: Option[LocalDateTime])(implicit val exchangeRateManager: ExchangeRateManager)

  // **************** Private inner types ****************//
  /** Number of cents. */
  type Value = Long

  private object ValueTransformer extends InputComponent.ValueTransformer[Value, ExtraProps] {
    override def stringToValue(string: String, extraProps: ExtraProps) = {
      StringArithmetic.floatStringToCents(string)
    }

    override def valueToString(cents: Long, extraProps: ExtraProps) = {
      Money.centsToFloatString(cents)
    }

    override def isEmptyValue(value: Long) = value == 0
  }

  @visibleForTesting private[bootstrap] object StringArithmetic {
    private sealed abstract class Operation(val apply: (Long, Long) => Long, val toChar: Char)
    private case object Plus extends Operation(_ + _, '+')
    private case object Minus extends Operation(_ - _, '-')
    private case object Times extends Operation(_ * _ / 100, '*')
    private case object DividedBy extends Operation((lhs, rhs) => if (rhs == 0) 0 else lhs * 100 / rhs, '/')

    def floatStringToCents(string: String): Option[Long] = {
      def inner(string: String, operations: List[Operation]): Option[Long] = operations match {
        case Nil => Money.floatStringToCents(string)
        case operation :: otherOperations if string contains operation.toChar =>
          val parts = {
            val rawParts = Splitter.on(operation.toChar).split(string)
            if (rawParts.head.trim.isEmpty && (operation == Plus || operation == Minus)) {
              "0" +: rawParts.tail
            } else {
              rawParts
            }
          }
          val results = parts.map(part => inner(part, otherOperations))
          if (results.forall(_.isDefined)) {
            Some(results.map(_.get).reduceLeft(operation.apply))
          } else {
            None
          }
        case _ :: otherOperations => inner(string, otherOperations)
      }
      inner(string, operations = List(Plus, Minus, Times, DividedBy))
    }
  }
}

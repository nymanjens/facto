package app.flux.react.uielements.input.bootstrap

import hydro.common.GuavaReplacement.Splitter
import hydro.common.I18n
import hydro.common.Annotations.visibleForTesting
import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.CurrencyValueManager
import app.common.money.Money
import hydro.common.time.LocalDateTime
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.bootstrap.InputComponent
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object MoneyInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    valueChangeForPropsChange = (newProps, oldValue) => {
      newProps.extra.forceValue match {
        case Some(forceValue) => forceValue
        case None             => oldValue
      }
    },
    inputRenderer = (
        classes: Seq[String],
        name: String,
        valueString: String,
        onChange: String => Callback,
        extraProps: ExtraProps,
    ) => {
      def materializeInputIfValid(): Callback = {
        ValueTransformer.stringToValue(valueString, extraProps) match {
          case Some(cents) => onChange(ValueTransformer.valueToString(cents, extraProps))
          case None        => Callback.empty
        }
      }
      val value = ValueTransformer.stringToValue(valueString, extraProps) getOrElse 0L

      Bootstrap.InputGroup(
        Bootstrap.InputGroupAddon(
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
          ^.onBlur ==> ((event: ReactEventFromInput) => materializeInputIfValid()),
        ),
        ^^.ifDefined(extraProps.addon(value))(t => Bootstrap.InputGroupAddon(t)),
      )
    },
  )

  // **************** API ****************//
  def apply(
      ref: Reference,
      name: String,
      label: String,
      defaultValue: Long,
      required: Boolean = false,
      showErrorMessage: Boolean,
      inputClasses: Seq[String] = Seq(),
      forceValue: Option[Long] = None,
      currency: Currency,
      listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance,
      addon: Value => Option[VdomNode] = _ => None,
  )(implicit
      currencyValueManager: CurrencyValueManager,
      i18n: I18n,
  ): VdomElement = {
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
        addon = addon,
      ),
      valueTransformer = ValueTransformer,
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput] (
      private[MoneyInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps]
  ) extends InputComponent.Reference(mutableRef)

  case class ExtraProps(
      forceValue: Option[Long],
      currency: Currency,
      addon: Value => Option[VdomNode],
  )(implicit val currencyValueManager: CurrencyValueManager)

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
    private sealed abstract class Operation(val apply: (Double, Double) => Double, val toChar: Char)
    private case object Plus extends Operation(_ + _, '+')
    private case object Minus extends Operation(_ - _, '-')
    private case object Times extends Operation(_ * _, '*')
    private case object DividedBy extends Operation((lhs, rhs) => if (rhs == 0) 0 else lhs / rhs, '/')

    def floatStringToCents(string: String): Option[Long] = {
      def inner(string: String, operations: List[Operation]): Option[Double] = operations match {
        case Nil =>
          Try(string.trim.toDouble) match {
            case Success(value) => Some(value)
            case Failure(_)     => Money.floatStringToCents(string).map(_ / 100.0)
          }
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
      inner(string, operations = List(Plus, Minus, Times, DividedBy)).map(Money.floatToCents)
    }
  }
}

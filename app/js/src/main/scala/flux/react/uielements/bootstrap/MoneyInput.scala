package flux.react.uielements.bootstrap

import common.I18n
import common.time.LocalDateTime
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
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
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventFromInput => Callback,
                               extraProps: ExtraProps) = {
        val referenceMoney = {
          val datedMoney = {
            val cents = ValueTransformer.stringToValue(valueString, extraProps) getOrElse 0L
            DatedMoney(cents, extraProps.currency, extraProps.date)
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
          ^^.ifThen(extraProps.currency.isForeign) {
            <.span(
              ^.className := "input-group-addon",
              <.i(^.className := Currency.default.iconClass),
              <.span(" " + referenceMoney.formatFloat)
            )
          }
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
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
      i18n: I18n): VdomElement = {
    val props = Props[Value, ExtraProps](
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps(forceValue = forceValue, currency = currency, date = date),
      valueTransformer = ValueTransformer
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput] (
      private[MoneyInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps(forceValue: Option[Long], currency: Currency, date: LocalDateTime)(
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

    override def emptyValue = 0
  }
}

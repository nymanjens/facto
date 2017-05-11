package flux.react.uielements.bootstrap

import common.time.LocalDateTime
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money.{Currency, DatedMoney, ExchangeRateManager, Money}

import scala.collection.immutable.Seq

object MoneyInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventI => Callback,
                               extraProps: ExtraProps) = {
        val referenceMoney = {
          val datedMoney = {
            val cents = ValueTransformer.stringToValue(valueString, extraProps) getOrElse 0L
            DatedMoney(cents, extraProps.currency, extraProps.date)
          }
          datedMoney.exchangedForReferenceCurrency(extraProps.exchangeRateManager)
        }

        <.div(^.className := "input-group",
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
            ^.onChange ==> onChange
          ),
          ^^.ifThen(extraProps.currency.isForeign) {
            <.span(
              ^.className := "input-group-addon",
              <.i(^.className := Currency.default.iconClass),
              <.span(referenceMoney.formatFloat)
            )
          }
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            label: String,
            help: String = null,
            errorMessage: String = null,
            inputClasses: Seq[String] = Seq(),
            currency: Currency,
            date: LocalDateTime,
            listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance)(
             implicit exchangeRateManager: ExchangeRateManager): ReactElement = {
    val props = Props[Value, ExtraProps](
      label = label,
      name = ref.name,
      defaultValue = 0,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps(
        currency,
        date),
      valueTransformer = ValueTransformer)
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput](refComp: InputComponent.ThisRefComp[Value, ExtraProps])
    extends InputComponent.Reference(refComp)

  case class ExtraProps(currency: Currency,
                        date: LocalDateTime)(
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
  }
}

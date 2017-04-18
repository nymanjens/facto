package flux.react.uielements.bootstrap

import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money.{Currency, Money}

import scala.collection.immutable.Seq

object MoneyInput {

  private val component = InputComponent.create(
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               value: String,
                               onChange: ReactEventI => Callback,
                               extraProps: ExtraProps) = {
        <.div(^.className := "input-group",
          <.span(
            ^.className := "input-group-addon currency-indicator",
            <.i(^.className := extraProps.currency.iconClass)
          ),
          <.input(
            ^.tpe := "text",
            ^.autoComplete := "off",
            ^^.classes(classes),
            ^.name := name,
            ^.value := value,
            ^.onChange ==> onChange
          ),
          <.span(
            ^.className := "input-group-addon",
            <.i(^.className := Currency.default.iconClass),
            <.span("0.00")
          )
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            label: String,
            defaultValue: String = "",
            help: String = null,
            errorMessage: String = null,
            inputClasses: Seq[String] = Seq(),
            currency: Currency,
            listener: InputBase.Listener = InputBase.Listener.nullInstance): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses,
      listener = listener,
      extra = ExtraProps(currency))
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[MoneyInput](refComp: InputComponent.ThisRefComp[ExtraProps])
    extends InputComponent.Reference(refComp)

  case class ExtraProps(currency: Currency)

  // **************** Private inner types ****************//
  private object ValueCleaner extends InputComponent.ValueCleaner[ExtraProps] {
    override def cleanupValue(internalValue: String, extraProps: ExtraProps) = {
      val longValue = Money.floatStringToCents(internalValue) getOrElse 0L
      longValue.toString
    }
  }
}

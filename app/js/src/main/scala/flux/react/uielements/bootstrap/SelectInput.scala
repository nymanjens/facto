package flux.react.uielements.bootstrap

import java.util.NoSuchElementException

import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
import flux.react.uielements.InputBase
import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.{ListMap, Seq}

object SelectInput {

  private val component = InputComponent.create(
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               value: String,
                               onChange: ReactEventI => Callback,
                               extraProps: ExtraProps) = {
        require(extraProps.optionValueToName.contains(value), s"Value '$value' is not a a valid option (${extraProps.optionValueToName.keys})")

        <.select(
          ^^.classes(classes),
          ^.name := name,
          ^.onChange ==> onChange,
          for ((optionValue, optionName) <- extraProps.optionValueToName) yield {
            <.option(
              ^.value := optionValue,
              ^.selected := value == optionValue,
              optionName
            )
          }
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
            optionValueToName: ListMap[String, String]): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses,
      extra = ExtraProps(optionValueToName),
      valueCleaner = ValueCleaner)
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[SelectInput](refComp: InputComponent.ThisRefComp[ExtraProps])
    extends InputComponent.Reference[ExtraProps](refComp)

  case class ExtraProps(optionValueToName: ListMap[String, String])

  // **************** Private inner types ****************//
  private object ValueCleaner extends InputComponent.ValueCleaner[ExtraProps] {
    override def cleanupValue(internalValue: String, extraProps: ExtraProps) = {
      if (extraProps.optionValueToName.contains(internalValue)) {
        internalValue
      } else {
        extraProps.optionValueToName.keys.headOption getOrElse ""
      }
    }
  }
}

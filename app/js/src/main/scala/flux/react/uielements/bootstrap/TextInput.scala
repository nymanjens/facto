package flux.react.uielements.bootstrap

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props, ValueTransformer}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object TextInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventFromInput => Callback,
                               extraProps: ExtraProps) = {
        <.input(
          ^.tpe := "text",
          ^^.classes(classes),
          ^.name := name,
          ^.value := valueString,
          ^.onChange ==> onChange
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            label: String,
            defaultValue: String = "",
            required: Boolean = false,
            showErrorMessage: Boolean,
            inputClasses: Seq[String] = Seq(),
            focusOnMount: Boolean = false,
            listener: InputBase.Listener[String] = InputBase.Listener.nullInstance)(
      implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = "TODO: add name parameter to TextInput",
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      focusOnMount = focusOnMount,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (
      private[TextInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
  private type Value = String
}

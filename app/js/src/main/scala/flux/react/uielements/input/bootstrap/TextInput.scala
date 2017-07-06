package flux.react.uielements.input.bootstrap

import common.I18n
import flux.react.ReactVdomUtils.^^
import InputComponent.{InputRenderer, Props, ValueTransformer}
import flux.react.uielements.input.InputBase
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
          ^.onChange ==> onChange,
          ^.autoFocus := extraProps.focusOnMount
        )
      }
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            name: String,
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
      name = name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance,
      extra = ExtraProps(focusOnMount = focusOnMount)
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (
      private[TextInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps private[TextInput] (private[TextInput] focusOnMount: Boolean)

  // **************** Private inner types ****************//
  private type Value = String
}

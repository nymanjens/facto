package hydro.flux.react.uielements.input.bootstrap

import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import hydro.flux.react.uielements.input.bootstrap.InputComponent.ValueTransformer
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

import scala.collection.immutable.Seq

object TextAreaInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = (
        classes: Seq[String],
        name: String,
        valueString: String,
        onChange: String => Callback,
        extraProps: ExtraProps,
    ) => {
      <.textarea(
        ^^.classes(classes),
        ^.name := name,
        ^.value := valueString,
        ^.rows := 2,
        ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value))
      )
    }
  )

  // **************** API ****************//
  def apply(
      ref: Reference,
      name: String,
      label: String,
      defaultValue: String = "",
      required: Boolean = false,
      showErrorMessage: Boolean,
      inputClasses: Seq[String] = Seq(),
      listener: InputBase.Listener[String] = InputBase.Listener.nullInstance,
  )(implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[TextAreaInput] (
      private[TextAreaInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
  private type Value = String
}

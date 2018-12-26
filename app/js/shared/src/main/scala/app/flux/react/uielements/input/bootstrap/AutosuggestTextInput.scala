package app.flux.react.uielements.input.bootstrap

import common.I18n
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import hydro.flux.react.uielements.input.bootstrap.InputComponent.ValueTransformer
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputValidator
import hydro.flux.react.uielements.input.bootstrap.InputComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import jsfacades.ReactAutosuggest

import scala.collection.immutable.Seq
import scala.scalajs.js

object AutosuggestTextInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = (classes: Seq[String],
                     name: String,
                     valueString: String,
                     onChange: String => Callback,
                     extraProps: ExtraProps) => {
      ReactAutosuggest(
        suggestions = extraProps.suggestions,
        onSuggestionsFetchRequested = extraProps.onSuggestionsFetchRequested,
        onSuggestionsClearRequested = extraProps.onSuggestionsClearRequested,
        renderSuggestion = suggestion => React.createElement("a", null, suggestion),
        inputProps = ReactAutosuggest.InputProps(
          value = valueString,
          onChange = newString => onChange(newString).runNow(),
          name = name,
          classes = classes
        ),
        theme = ReactAutosuggest.Theme(
          container = "autosuggest",
          input = "form-control",
          suggestionsContainer = "dropdown open",
          suggestionsList = "dropdown-menu",
          suggestion = "",
          suggestionHighlighted = "active"
        )
      )
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            name: String,
            label: String,
            defaultValue: String = "",
            required: Boolean = false,
            showErrorMessage: Boolean = false,
            additionalValidator: InputValidator[String] = InputValidator.alwaysValid,
            inputClasses: Seq[String] = Seq(),
            suggestions: Seq[String],
            onSuggestionsFetchRequested: String => Unit,
            onSuggestionsClearRequested: () => Unit,
            listener: InputBase.Listener[String] = InputBase.Listener.nullInstance)(
      implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      additionalValidator = additionalValidator,
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance,
      extra = ExtraProps(
        suggestions = suggestions,
        onSuggestionsFetchRequested = onSuggestionsFetchRequested,
        onSuggestionsClearRequested = onSuggestionsClearRequested)
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[AutosuggestTextInput] (
      private[AutosuggestTextInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps private[AutosuggestTextInput] (suggestions: Seq[String],
                                                       onSuggestionsFetchRequested: String => Unit,
                                                       onSuggestionsClearRequested: () => Unit)

  // **************** Private inner types ****************//
  private type Value = String
}

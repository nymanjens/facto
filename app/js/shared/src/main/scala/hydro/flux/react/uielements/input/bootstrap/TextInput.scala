package hydro.flux.react.uielements.input.bootstrap

import java.time.Duration
import hydro.common.DesktopKeyCombination
import hydro.common.DesktopKeyCombination.ArrowDown
import hydro.common.DesktopKeyCombination.SpecialKey
import hydro.common.DesktopKeyCombination.ArrowUp
import hydro.common.I18n
import hydro.common.time.LocalDateTime
import hydro.common.time.TimeUtils
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputValidator
import hydro.flux.react.uielements.input.bootstrap.InputComponent.CleanupCharactersOptions
import hydro.flux.react.uielements.input.bootstrap.InputComponent.Props
import hydro.flux.react.uielements.input.bootstrap.InputComponent.ValueTransformer
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.SyntheticKeyboardEvent
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object TextInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = (
        classes: Seq[String],
        name: String,
        valueString: String,
        onChange: String => Callback,
        extraProps: ExtraProps,
    ) => {
      <.input(
        ^.tpe := extraProps.inputType,
        ^^.classes(classes),
        ^.name := name,
        ^.value := valueString,
        ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value)),
        ^.autoFocus := extraProps.focusOnMount,
        ^^.ifThen(!extraProps.autoComplete) {
          ^.autoComplete.off
        },
        ^.disabled := extraProps.disabled,
        ^^.ifDefined(extraProps.arrowHandler) { arrowHandler =>
          ^.onKeyDown ==> handleKeyDown(arrowHandler, currentValue = valueString, onChange = onChange),
        },
      )
    },
  )

  // **************** API ****************//
  def apply(
      ref: Reference,
      name: String,
      label: String,
      inputType: String = "text",
      defaultValue: String = "",
      required: Boolean = false,
      showErrorMessage: Boolean = false,
      additionalValidator: InputValidator[String] = InputValidator.alwaysValid,
      inputClasses: Seq[String] = Seq(),
      focusOnMount: Boolean = false,
      autoComplete: Boolean = true,
      disabled: Boolean = false,
      arrowHandler: ArrowHandler = null,
      listener: InputBase.Listener[String] = InputBase.Listener.nullInstance,
      substituteNonLatin1: Boolean = true,
  )(implicit i18n: I18n): VdomElement = {
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
      cleanupSpecializedCharacters =
        Some(CleanupCharactersOptions(stripNewlines = true, substituteNonLatin1 = substituteNonLatin1)),
      extra = ExtraProps(
        inputType = inputType,
        focusOnMount = focusOnMount,
        autoComplete = autoComplete,
        disabled = disabled,
        arrowHandler = Option(arrowHandler),
      ),
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  trait ArrowHandler {
    def newValueOnArrowUp(currentValue: String): String
    def newValueOnArrowDown(currentValue: String): String
  }
  object ArrowHandler {
    object DateHandler extends ArrowHandler {
      override def newValueOnArrowUp(currentValue: String): String = {
        newValueOnDelta(daysDelta = 1, currentValue)
      }
      override def newValueOnArrowDown(currentValue: String): String = {
        newValueOnDelta(daysDelta = -1, currentValue)
      }

      private def newValueOnDelta(daysDelta: Int, currentValue: String): String = {
        try {
          val currentDate = TimeUtils.parseDateString(currentValue.trim)
          val newDate = currentDate.plus(Duration.ofDays(daysDelta))
          newDate.toLocalDate.toString
        } catch {
          case _: IllegalArgumentException => currentValue
        }
      }
    }
  }

  final class Reference private[TextInput] (
      private[TextInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps]
  ) extends InputComponent.Reference(mutableRef)

  case class ExtraProps private[TextInput] (
      inputType: String,
      focusOnMount: Boolean,
      autoComplete: Boolean,
      disabled: Boolean,
      arrowHandler: Option[ArrowHandler],
  )

  // **************** Private helper methods ****************//
  private def handleKeyDown(
      arrowHandler: ArrowHandler,
      currentValue: String,
      onChange: String => Callback,
  )(event: SyntheticKeyboardEvent[_]): Callback = {
    val keyCombination = DesktopKeyCombination.fromEvent(event)

    keyCombination match {
      case special @ SpecialKey(
            ArrowUp | ArrowDown, /* ctrl */ false, /* shift */ false, /* alt */ false, /* meta */ false,
          ) =>
        val newValue =
          special.specialKeyType match {
            case ArrowUp   => arrowHandler.newValueOnArrowUp(currentValue)
            case ArrowDown => arrowHandler.newValueOnArrowDown(currentValue)
            case _         => throw new AssertionError(special)
          }
        if (currentValue == newValue) {
          Callback.empty
        } else {
          event.preventDefault()
          onChange(newValue)
        }

      case _ => Callback.empty
    }
  }

  // **************** Private inner types ****************//
  private type Value = String
}

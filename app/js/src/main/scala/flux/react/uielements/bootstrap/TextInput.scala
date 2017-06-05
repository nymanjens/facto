package flux.react.uielements.bootstrap

import japgolly.scalajs.react.component.Scala.MutableRef
import java.time.LocalDate
import java.util.NoSuchElementException

import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props, ValueTransformer}
import flux.react.uielements.InputBase
import common.{I18n, LoggingUtils}
import common.time.TimeUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import org.scalajs.dom.html

import scala.collection.immutable.Seq
import scala.reflect.ClassTag
import scala.util.Try

object TextInput {

  private val inputRef: RefSimple[HTMLElement] = Ref[HTMLElement]("inputRef")

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
          ^.ref := inputRef
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
      name = ref.name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      focusOnMount = if (focusOnMount) Some(inputRef) else None,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance
    )
    ref.mutableRef.component(props)
  }

  def ref(name: String): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
  private type Value = String
}

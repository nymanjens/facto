package flux.react.uielements.bootstrap

import java.time.LocalDate
import java.util.NoSuchElementException

import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props, ValueTransformer}
import flux.react.uielements.InputBase
import common.{I18n, LoggingUtils}
import common.time.TimeUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import japgolly.scalajs.react.TopNode
import org.scalajs.dom.html

import scala.collection.immutable.Seq
import scala.reflect.ClassTag
import scala.util.Try

object TextAreaInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventI => Callback,
                               extraProps: ExtraProps) = {
        <.textarea(
          ^^.classes(classes),
          ^.name := name,
          ^.value := valueString,
          ^.rows := 2,
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
            listener: InputBase.Listener[String] = InputBase.Listener.nullInstance)(
      implicit i18n: I18n): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance
    )
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TextAreaInput] (refComp: InputComponent.ThisRefComp[Value, ExtraProps])
      extends InputComponent.Reference(refComp)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
  private type Value = String
}

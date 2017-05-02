package flux.react.uielements.bootstrap

import java.time.LocalDate
import java.util.NoSuchElementException

import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props, ValueTransformer}
import flux.react.uielements.InputBase
import common.LoggingUtils
import common.time.TimeUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq
import scala.reflect.ClassTag
import scala.util.Try

class TextInput[Value] private(valueTransformer: ValueTransformer[Value, Unit])(implicit valueTag: ClassTag[Value]) {

  private val component = InputComponent.create[Value, ExtraProps](
    name = s"${getClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}",
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               valueString: String,
                               onChange: ReactEventI => Callback,
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
            defaultValue: Value,
            help: String = null,
            errorMessage: String = null,
            inputClasses: Seq[String] = Seq(),
            listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = valueTransformer)
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TextInput](refComp: InputComponent.ThisRefComp[Value, ExtraProps])
    extends InputComponent.Reference(refComp)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
}

object TextInput {
  val general = new TextInput[String](ValueTransformer.nullInstance)
  val forDate = new TextInput[LocalDate](LocalDateTransformer)

  private object LocalDateTransformer extends ValueTransformer[LocalDate, Unit] {
    override def stringToValue(string: String, extraProps: Unit) = {
      try {
        Some(TimeUtils.parseDateString(string).toLocalDate)
      } catch {
        case _: IllegalArgumentException => None
      }
    }
    override def valueToString(value: LocalDate, extraProps: Unit) = value.toString
  }
}
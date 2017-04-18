package flux.react.uielements.bootstrap

import java.util.NoSuchElementException

import flux.react.uielements.bootstrap.InputComponent.{InputRenderer, Props}
import flux.react.uielements.InputBase
import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{^^, <<}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq

object TextInput {

  private val component = InputComponent.create(
    name = getClass.getSimpleName,
    inputRenderer = new InputRenderer[ExtraProps] {
      override def renderInput(classes: Seq[String],
                               name: String,
                               value: String,
                               onChange: ReactEventI => Callback,
                               extraProps: ExtraProps) = {
        <.input(
          ^.tpe := "text",
          ^^.classes(classes),
          ^.name := name,
          ^.value := value,
          ^.onChange ==> onChange
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
            listener: InputBase.Listener = InputBase.Listener.nullInstance): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses,
      listener = listener)
    component.withRef(ref.name)(props)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TextInput](refComp: InputComponent.ThisRefComp[ExtraProps])
    extends InputComponent.Reference(refComp)

  // **************** Private inner types ****************//
  private type ExtraProps = Unit
}

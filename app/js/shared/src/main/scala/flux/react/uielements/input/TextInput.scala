package flux.react.uielements.input

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

import scala.collection.immutable.Seq

object TextInput {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(value = ""))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(ref: Reference,
            name: String,
            placeholder: String = "",
            classes: Seq[String] = Seq()): VdomElement = {
    val props = Props(name = name, placeholder = placeholder, classes = classes)
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (private[TextInput] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[String] {
    override def apply(): InputBase.Proxy[String] = {
      Option(mutableRef.unsafeGet()) map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }

  // **************** Private inner types ****************//
  private case class Props private[TextInput] (name: String, placeholder: String, classes: Seq[String])
  private case class State(value: String) {
    def withValue(newValue: String): State = copy(value = newValue)
  }

  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisMutableRef = ToScalaComponent[Props, State, Backend, ThisCtorSummoner#CT]
  private type ThisComponentU = MountedImpure[Props, State, Backend]

  private final class Proxy(val component: ThisComponentU) extends InputBase.Proxy[String] {
    override def value = component.state.value match {
      case ""    => None
      case value => Some(value)
    }
    override def valueOrDefault = value getOrElse ""
    override def setValue(newValue: String) = {
      component.modState(_.withValue(newValue))
      newValue
    }

    override def registerListener(listener: InputBase.Listener[String]) = ???
    override def deregisterListener(listener: InputBase.Listener[String]) = ???

    override def focus(): Unit = {
      component.backend.theInput.unsafeGet().focus()
    }
  }

  private class Backend($ : BackendScope[Props, State]) {
    val theInput = Ref[html.Input]

    def render(props: Props, state: State) = logExceptions {
      <.input(
        ^.tpe := "text",
        ^.name := props.name,
        ^^.classes(props.classes),
        ^.value := state.value,
        ^.placeholder := props.placeholder,
        ^.onChange ==> { (e: ReactEventFromInput) =>
          LogExceptionsCallback {
            val newString = e.target.value
            $.modState(_.withValue(newString)).runNow()
          }
        }
      ).withRef(theInput)
    }
  }
}

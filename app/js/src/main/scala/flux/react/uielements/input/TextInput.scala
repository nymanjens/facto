package flux.react.uielements.input

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.{MountedImpure, MutableRef}
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._

object TextInput {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(value = ""))
    .renderPS((context, props, state) =>
      logExceptions {
        <.input(
          ^.tpe := "text",
          ^.name := props.name,
          ^.value := state.value,
          ^.onChange ==> { (e: ReactEventFromInput) =>
            LogExceptionsCallback {
              val newString = e.target.value
              context.modState(_.withValue(newString)).runNow()
            }
          }
        )
    })
    .build

  // **************** API ****************//
  def apply(ref: Reference, name: String, placeholder: String = ""): VdomElement = {
    val props = Props(name = name, placeholder = placeholder)
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (private[TextInput] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[String] {
    override def apply(): InputBase.Proxy[String] = {
      Option(mutableRef.value) map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }

  // **************** Private inner types ****************//
  private case class Props private[TextInput] (name: String, placeholder: String)
  private case class State(value: String) {
    def withValue(newValue: String): State = copy(value = newValue)
  }

  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisMutableRef = MutableRef[Props, State, Backend, ThisCtorSummoner#CT]
  private type Backend = Unit
  private type ThisComponentU = MountedImpure[Props, State, Backend]

  private final class Proxy(val component: ThisComponentU) extends InputBase.Proxy[String] {
    override def value = component.state.value match {
      case "" => None
      case value => Some(value)
    }
    override def valueOrDefault = value getOrElse ""
    override def setValue(newValue: String) = {
      component.modState(_.withValue(newValue))
      newValue
    }

    override def registerListener(listener: InputBase.Listener[String]) = ???
    override def deregisterListener(listener: InputBase.Listener[String]) = ???
  }
}

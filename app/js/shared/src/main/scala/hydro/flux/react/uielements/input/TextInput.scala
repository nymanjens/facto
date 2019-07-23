package hydro.flux.react.uielements.input

import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.vdom.html_<^.^
import org.scalajs.dom.console
import org.scalajs.dom.html

import scala.collection.immutable.Seq

object TextInput extends HydroReactComponent {

  // **************** API ****************//
  def apply(
      ref: Reference,
      name: String,
      placeholder: String = "",
      classes: Seq[String] = Seq(),
      focusOnMount: Boolean = false,
  ): VdomElement = {
    val props = Props(name = name, placeholder = placeholder, classes = classes, focusOnMount = focusOnMount)
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (private[TextInput] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[String] {
    override def apply(): InputBase.Proxy[String] = {
      mutableRef.get.asCallback.runNow() map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props private[TextInput] (
      name: String,
      placeholder: String,
      classes: Seq[String],
      focusOnMount: Boolean,
  )
  protected case class State(value: String = "") {
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
      component.backend.theInput.get.asCallback.runNow() match {
        case Some(input) => input.focus()
        case None        => console.log("Warning: Could not focus because input not found")
      }
    }
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    val theInput = Ref[html.Input]

    override def render(props: Props, state: State) = logExceptions {
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
        },
        ^.autoFocus := props.focusOnMount,
      ).withRef(theInput)
    }
  }
}

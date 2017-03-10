package flux.react.uielements.bootstrap

import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq

object TextInput {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState_P(props => props.defaultValue)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(ref: Reference): ReactElement = {
    component.withRef(ref.refComp)(Props("label", "defaultValue"))
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TextInput](private[TextInput] val refComp: RefComp[Props, State, Backend, _ <: TopNode]) {
    def apply($: BackendScope[_, _]): ComponentProxy = new ComponentProxy(() => refComp($).get)
  }

  final class ComponentProxy private[TextInput](private val componentProvider: () => ReactComponentU[Props, State, Backend, _ <: TopNode]) {
    def value: String = componentProvider().state
    def setValue(string: String): Unit = componentProvider().modState(_ => string)
  }

  // **************** Private inner types ****************//
  private type State = String

  private case class Props(label: String, defaultValue: String = "", help: String = "", errorMessage: Option[String] = None)


  private final class Backend($: BackendScope[Props, State]) {
    def onChange(e: ReactEventI) = $.setState(e.target.value)

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      <.div(^.className := "form-group @if(field.hasErrors) {has-error}",
        <.label(^.className := "col-sm-4 control-label", props.label),
        <.div(^.className := "col-sm-8",
          <.input(
            ^.tpe := "text",
            ^.className := "form-control",
            ^.id := "@field.id",
            ^.name := "@field.name",
            ^.value := state,
            ^.placeholder := "@placeholder",
            ^.onChange ==> onChange),
          <.span(^.className := "help-block", props.help),
          <.span(^.className := "help-block", props.errorMessage)
        )
      )
    }
  }
}

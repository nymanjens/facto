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
  def apply(label: String,
            name: String,
            defaultValue: String = "",
            help: String = null,
            errorMessage: String = null,
            inputClasses: Seq[String] = Seq(),
            onChange: String => Callback = _ => Callback(),
            ref: Reference = null): ReactElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses=inputClasses,
      onChange = onChange)
    if (ref == null) {
      component(props)
    } else {
      component.withRef(ref.refComp)(props)
    }
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

  private case class Props(label: String,
                           name: String,
                           defaultValue: String,
                           help: Option[String],
                           errorMessage: Option[String],
                           inputClasses: Seq[String],
                           onChange: String => Callback)

  private final class Backend($: BackendScope[Props, State]) {
    def onChange(e: ReactEventI): Callback = Callback {
      val newValue = e.target.value
      $.props.runNow().onChange(newValue).runNow()
      $.setState(newValue).runNow()
    }

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      <.div(
        ^^.classes("form-group", props.errorMessage.map(_ => "has-error") getOrElse ""),
        <.label(^.className := "col-sm-4 control-label", props.label),
        <.div(
          ^.className := "col-sm-8",
          <.input(
            ^.tpe := "text",
            ^^.classes("form-control" +: props.inputClasses),
            ^.id := props.name,
            ^.name := props.name,
            ^.value := state,
            ^.onChange ==> onChange),
          ^^.ifThen(props.help) { msg =>
            <.span(^.className := "help-block", msg)
          },
          ^^.ifThen(props.errorMessage) { msg =>
            <.span(^.className := "help-block", msg)
          }
        )
      )
    }
  }
}

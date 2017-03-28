package flux.react.uielements.bootstrap

import java.util.NoSuchElementException

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

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState_P[State](props => State(value = props.defaultValue))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(label: String,
            defaultValue: String = "",
            help: String = null,
            errorMessage: String = null,
            inputClasses: Seq[String] = Seq(),
            ref: Reference = null): ReactElement = {
    val props = Props(
      label = label,
      name = ref.name,
      defaultValue = defaultValue,
      help = Option(help),
      errorMessage = Option(errorMessage),
      inputClasses = inputClasses)
    if (ref == null) {
      component(props)
    } else {
      component.withRef(ref.name)(props)
    }
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  final class Reference private[TextInput](refComp: ThisRefComp) extends InputBase.Reference {
    override def apply($: BackendScope[_, _]): InputBase.Proxy = new Proxy(() => refComp($).get)
    override def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type ThisRefComp = RefComp[Props, State, Backend, _ <: TopNode]
  private type ThisComponentU = ReactComponentU[Props, State, Backend, _ <: TopNode]


  private final class Proxy(val componentProvider: () => ThisComponentU) extends InputBase.Proxy {
    override def value = componentProvider().state.value
    override def setValue(string: String) = componentProvider().modState(_.withValue(string))
    override def registerListener(listener: InputBase.Listener) = componentProvider().modState(_.withListener(listener))
    override def deregisterListener(listener: InputBase.Listener) = {
      try {
        componentProvider().modState(_.withoutListener(listener))
      } catch {
        case _: NoSuchElementException => // Ignore the case this component no longer exists
      }
    }
  }

  private case class State(value: String, listeners: Seq[InputBase.Listener] = Seq()) {
    def withValue(newValue: String): State = copy(value = newValue)
    def withListener(listener: InputBase.Listener): State = copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener): State = copy(listeners = listeners.filter(_ != listener))
  }

  private case class Props(label: String,
                           name: String,
                           defaultValue: String,
                           help: Option[String],
                           errorMessage: Option[String],
                           inputClasses: Seq[String])

  private final class Backend($: BackendScope[Props, State]) {
    def onChange(e: ReactEventI): Callback = Callback {
      val newValue = e.target.value
      for (listener <- $.state.runNow().listeners) {
        listener.onChange(newValue).runNow()
      }
      $.modState(_.withValue(newValue)).runNow()
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
            ^.value := state.value,
            ^.onChange ==> onChange),
          <<.ifThen(props.help) { msg =>
            <.span(^.className := "help-block", msg)
          },
          <<.ifThen(props.errorMessage) { msg =>
            <.span(^.className := "help-block", msg)
          }
        )
      )
    }
  }
}

package flux.react.uielements.bootstrap

import java.util.NoSuchElementException

import flux.react.uielements.InputBase
import common.LoggingUtils
import japgolly.scalajs.react.{ReactEventI, TopNode, _}
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq

private[bootstrap] object InputComponent {

  // **************** API ****************//
  def create[ExtraProps](name: String,
                         inputRenderer: InputRenderer[ExtraProps]) = {
    ReactComponentB[Props[ExtraProps]](name)
      .initialState_P[State](props => State(value = props.defaultValue))
      .renderPS((context, props, state) => LoggingUtils.logExceptions {
        def onChange(e: ReactEventI): Callback = Callback {
          LoggingUtils.logExceptions {
            val newValue = e.target.value
            for (listener <- context.state.listeners) {
              listener.onChange(newValue, directUserChange = true).runNow()
            }
            context.modState(_.withValue(newValue)).runNow()
          }
        }

        <.div(
          ^^.classes(Seq("form-group") ++ props.errorMessage.map(_ => "has-error")),
          <.label(
            ^.className := "col-sm-4 control-label",
            props.label),
          <.div(
            ^.className := "col-sm-8",
            inputRenderer.renderInput(
              classes = "form-control" +: props.inputClasses,
              name = props.name,
              value = state.value,
              onChange = onChange,
              extraProps = props.extra),
            <<.ifThen(props.help) { msg =>
              <.span(^.className := "help-block", msg)
            },
            <<.ifThen(props.errorMessage) { msg =>
              <.span(^.className := "help-block", msg)
            }
          )
        )
      })
      .build
  }

  // **************** Public inner types ****************//
  type ThisRefComp = RefComp[_ <: Props[_], State, Backend, _ <: TopNode]

  trait InputRenderer[ExtraProps] {
    def renderInput(classes: Seq[String],
                    name: String,
                    value: String,
                    onChange: ReactEventI => Callback,
                    extraProps: ExtraProps): ReactNode
  }

  case class Props[ExtraProps](label: String,
                               name: String,
                               defaultValue: String,
                               help: Option[String],
                               errorMessage: Option[String],
                               inputClasses: Seq[String],
                               extra: ExtraProps = Unit)
  case class State(value: String,
                   listeners: Seq[InputBase.Listener] = Seq()) {
    def withValue(newValue: String): State = copy(value = newValue)
    def withListener(listener: InputBase.Listener): State = copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener): State = copy(listeners = listeners.filter(_ != listener))
  }


  abstract class Reference(refComp: ThisRefComp) extends InputBase.Reference {
    override final def apply($: BackendScope[_, _]): InputBase.Proxy = new Proxy(() => refComp($).get)
    override final def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type Backend = Unit
  private type ThisComponentU = ReactComponentU[_ <: Props[_], State, Backend, _ <: TopNode]

  private final class Proxy(val componentProvider: () => ThisComponentU) extends InputBase.Proxy {
    override def value = componentProvider().state.value
    override def setValue(string: String) = {
      componentProvider().modState(_.withValue(string))
      for (listener <- componentProvider().state.listeners) {
        listener.onChange(string, directUserChange = false).runNow()
      }
    }
    override def registerListener(listener: InputBase.Listener) = componentProvider().modState(_.withListener(listener))
    override def deregisterListener(listener: InputBase.Listener) = {
      try {
        componentProvider().modState(_.withoutListener(listener))
      } catch {
        case _: NoSuchElementException => // Ignore the case this component no longer exists
      }
    }
  }
}

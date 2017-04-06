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
            val cleanedNewValue = ValueCleaner.cleanupValue(newValue, props)
            val cleanedOldValue = ValueCleaner.cleanupValue(state.value, props)
            if (cleanedOldValue != cleanedNewValue) {
              for (listener <- context.state.listeners) {
                listener.onChange(cleanedNewValue, directUserChange = true).runNow()
              }
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
      .componentWillReceiveProps(scope => Callback {
        LoggingUtils.logExceptions {
          // If the props have changed, the cleaned value may have changed. If this happens, the listeners should
          // be notified.
          val value = scope.currentState.value
          val currentCleanedValue = ValueCleaner.cleanupValue(value, scope.currentProps)
          val newCleanedValue = ValueCleaner.cleanupValue(value, scope.nextProps)
          if (currentCleanedValue != newCleanedValue) {
            for (listener <- scope.currentState.listeners) {
              listener.onChange(newCleanedValue, directUserChange = false).runNow()
            }
          }
        }
      })
      .build
  }

  // **************** Public inner types ****************//
  type ThisRefComp[ExtraProps] = RefComp[Props[ExtraProps], State, Backend, _ <: TopNode]

  trait InputRenderer[ExtraProps] {
    def renderInput(classes: Seq[String],
                    name: String,
                    value: String,
                    onChange: ReactEventI => Callback,
                    extraProps: ExtraProps): ReactNode
  }

  trait ValueCleaner[ExtraProps] {
    /**
      * Returns the value as it should be seen via `InputBase.Proxy.value` or as listener (i.e. the listener will
      * only listen to changes to the cleaned value, rather than the internal value).
      *
      * Note: This does not influence the possible values the input can have internally (in `state.value`).
      */
    def cleanupValue(internalValue: String, extraProps: ExtraProps): String
  }

  object ValueCleaner {
    def nullInstance[ExtraProps] = new ValueCleaner[ExtraProps] {
      override def cleanupValue(internalValue: String, extraProps: ExtraProps) = internalValue
    }

    def cleanupValue[ExtraProps](internalValue: String, props: Props[ExtraProps]): String = {
      props.valueCleaner.cleanupValue(internalValue, props.extra)
    }
  }

  case class Props[ExtraProps](label: String,
                               name: String,
                               defaultValue: String,
                               help: Option[String],
                               errorMessage: Option[String],
                               inputClasses: Seq[String],
                               extra: ExtraProps,
                               valueCleaner: ValueCleaner[ExtraProps])
  object Props {
    def apply(label: String,
              name: String,
              defaultValue: String,
              help: Option[String],
              errorMessage: Option[String],
              inputClasses: Seq[String],
              valueCleaner: ValueCleaner[Unit] = ValueCleaner.nullInstance): Props[Unit] = {
      new Props(
        label = label,
        name = name,
        defaultValue = defaultValue,
        help = help,
        errorMessage = errorMessage,
        inputClasses = inputClasses,
        extra = (): Unit,
        valueCleaner = valueCleaner)
    }
  }

  case class State(value: String,
                   listeners: Seq[InputBase.Listener] = Seq()) {
    def withValue(newValue: String): State = copy(value = newValue)
    def withListener(listener: InputBase.Listener): State = copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener): State = copy(listeners = listeners.filter(_ != listener))
  }

  abstract class Reference[ExtraProps](refComp: ThisRefComp[ExtraProps]) extends InputBase.Reference {
    override final def apply($: BackendScope[_, _]): InputBase.Proxy = new Proxy[ExtraProps](() => refComp($).get)
    override final def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type Backend = Unit
  private type ThisComponentU[ExtraProps] = ReactComponentU[Props[ExtraProps], State, Backend, _ <: TopNode]

  private final class Proxy[ExtraProps](val componentProvider: () => ThisComponentU[ExtraProps]) extends InputBase.Proxy {
    override def value = cleanupValue(componentProvider().state.value)
    override def setValue(string: String) = {
      if (string == cleanupValue(string)) {
        componentProvider().modState(_.withValue(string))
        for (listener <- componentProvider().state.listeners) {
          listener.onChange(string, directUserChange = false).runNow()
        }
      } else {
        println(s"  Setting a value ('$string') that is different when cleaned up (cleaned up value = '${cleanupValue(string)}'). " +
          s"Will ignore this setter.")
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

    private def cleanupValue(internalValue: String): String = {
      ValueCleaner.cleanupValue(internalValue, componentProvider().props)
    }
  }
}

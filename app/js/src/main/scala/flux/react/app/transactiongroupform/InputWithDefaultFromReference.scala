package flux.react.app.transactiongroupform

import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.ReactComponentC.ReqProps
import flux.react.uielements.bootstrap.TextInput
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq

private[transactiongroupform] object InputWithDefaultFromReference {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](ConnectionState.connectedToDefault)
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply(ref: InputWithDefaultFromReference.Reference,
            defaultValueProxy: TextInput.ComponentProxy
           )(inputElementFactory: InputElementExtraProps => ReactElement): ReactElement = {
    component.withRef(ref.refComp)(Props(
      inputElementRef = TextInput.ref(ref.refComp.name + "_input"),
      defaultValueProxy,
      inputElementFactory))
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  case class InputElementExtraProps(ref: TextInput.Reference, inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference](private[InputWithDefaultFromReference] val refComp: RefComp[Props, State, Backend, _ <: TopNode]) {
    def apply($: BackendScope[_, _]): ComponentProxy = new ComponentProxy(() => refComp($).get)
  }

  final class ComponentProxy private[InputWithDefaultFromReference](private val componentProvider: () => ReactComponentU[Props, State, Backend, _ <: TopNode]) {
    def input: TextInput.ComponentProxy = componentProvider().props.inputElementRef(componentProvider().backend.$)
  }

  // **************** Private inner types ****************//
  private type State = ConnectionState

  private case class Props(inputElementRef: TextInput.Reference,
                           defaultValueProxy: TextInput.ComponentProxy,
                           inputElementFactory: InputElementExtraProps => ReactElement)

  private final class Backend(val $: BackendScope[Props, State]) {

    def didMount(props: Props): Callback = Callback {
      LoggingUtils.logExceptions {
        props.inputElementRef($).registerListener(InputValueListener)
        props.defaultValueProxy.registerListener(DefaultValueListener)
      }
    }

    def willUnmount(props: Props): Callback = Callback {
      LoggingUtils.logExceptions {
        props.inputElementRef($).deregisterListener(InputValueListener)
        props.defaultValueProxy.deregisterListener(DefaultValueListener)
      }
    }

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
      props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses))
    }

    private object InputValueListener extends TextInput.InputListener {
      override def onChange(newInputValue: String) = Callback {
        LoggingUtils.logExceptions {
          val defaultValue = $.props.runNow().defaultValueProxy.value
          $.setState(ConnectionState(isConnected = defaultValue == newInputValue)).runNow()
        }
      }
    }

    private object DefaultValueListener extends TextInput.InputListener {
      override def onChange(newDefaultValue: String) = Callback {
        LoggingUtils.logExceptions {
          val inputProxy = $.props.runNow().inputElementRef($)
          val inputValue = inputProxy.value
          if ($.state.runNow().isConnected) {
            inputProxy.setValue(newDefaultValue)
          } else {
            $.setState(ConnectionState(isConnected = newDefaultValue == inputValue)).runNow()
          }
        }
      }
    }
  }

  private case class ConnectionState(isConnected: Boolean)
  private object ConnectionState {
    val connectedToDefault = ConnectionState(true)
    val disconnected = ConnectionState(true)
  }
}

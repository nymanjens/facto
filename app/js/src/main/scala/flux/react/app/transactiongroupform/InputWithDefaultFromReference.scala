package flux.react.app.transactiongroupform

import common.LoggingUtils
import flux.react.uielements.InputBase
import japgolly.scalajs.react.{TopNode, _}

import scala.collection.immutable.Seq

private[transactiongroupform] object InputWithDefaultFromReference {

  private val component = ReactComponentB[Props.any](getClass.getSimpleName)
    .initialState[State](ConnectionState.connectedToDefault)
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply[DelegateRef <: InputBase.Reference](ref: Reference,
                                                defaultValueProxy: => InputBase.Proxy,
                                                nameToDelegateRef: String => DelegateRef
                                               )(inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement): ReactElement = {
    component.withRef(ref.name)(Props(
      inputElementRef = nameToDelegateRef("delegate"),
      () => defaultValueProxy,
      inputElementFactory))
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference](ref: DelegateRef,
                                                                        inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference](refComp: ThisRefComp) extends InputBase.Reference {
    override def apply($: BackendScope[_, _]) = {
      InputBase.Proxy.forwardingTo {
        val component = refComp($).get
        val componentScope = component.backend.$
        component.props.inputElementRef(componentScope)
      }
    }
    override def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type ThisRefComp = RefComp[Props.any, State, Backend, _ <: TopNode]

  private type State = ConnectionState

  private case class Props[DelegateRef <: InputBase.Reference](inputElementRef: DelegateRef,
                                                               defaultValueProxy: () => InputBase.Proxy,
                                                               inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement)
  private object Props {
    type any = Props[_ <: InputBase.Reference]
  }

  private final class Backend(val $: BackendScope[Props.any, State]) {

    def didMount(props: Props.any): Callback = Callback {
      LoggingUtils.logExceptions {
        props.inputElementRef($).registerListener(InputValueListener)
        props.defaultValueProxy().registerListener(DefaultValueListener)
      }
    }

    def willUnmount(props: Props.any): Callback = Callback {
      LoggingUtils.logExceptions {
        props.inputElementRef($).deregisterListener(InputValueListener)
        props.defaultValueProxy().deregisterListener(DefaultValueListener)
      }
    }

    def render(props: Props.any, state: State) = LoggingUtils.logExceptions {
      def renderInternal[DelegateRef <: InputBase.Reference](props: Props[DelegateRef]) = {
        val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
        props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses))
      }
      renderInternal(props)
    }

    private object InputValueListener extends InputBase.Listener {
      override def onChange(newInputValue: String) = Callback {
        LoggingUtils.logExceptions {
          val defaultValue = $.props.runNow().defaultValueProxy().value
          $.setState(ConnectionState(isConnected = defaultValue == newInputValue)).runNow()
        }
      }
    }

    private object DefaultValueListener extends InputBase.Listener {
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

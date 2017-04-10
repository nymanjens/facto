package flux.react.app.transactiongroupform

import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import flux.react.uielements.InputBase
import japgolly.scalajs.react.{TopNode, _}

import scala.collection.immutable.Seq

private[transactiongroupform] object InputWithDefaultFromReference {

  private val component = ReactComponentB[Props.any]("InputWithDefaultFromReferenceWrapper")
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply[DelegateRef <: InputBase.Reference](ref: Reference,
                                                defaultValueProxy: Option[() => InputBase.Proxy],
                                                nameToDelegateRef: String => DelegateRef
                                               )(inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement): ReactElement = {
    component.withRef(ref.name)(Props(
      inputElementRef = nameToDelegateRef("delegate"),
      defaultValueProxy = defaultValueProxy,
      inputElementFactory = inputElementFactory))
  }

  def apply[DelegateRef <: InputBase.Reference](ref: Reference,
                                                defaultValueProxy: => InputBase.Proxy,
                                                nameToDelegateRef: String => DelegateRef
                                               )(inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement): ReactElement = {
    apply(
      ref = ref,
      defaultValueProxy = Some(() => defaultValueProxy),
      nameToDelegateRef = nameToDelegateRef)(
      inputElementFactory)
  }

  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference](ref: DelegateRef,
                                                                        inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference](refComp: ThisRefComp) extends InputBase.Reference {
    override def apply($: BackendScope[_, _]) = {
      InputBase.Proxy.forwardingTo {
        val component = refComp($).get
        val wrapperComponentScope = component.backend.$
        val props = component.props

        val implComponentScope = props.defaultValueProxy match {
          case Some(_) => Impl.ref(wrapperComponentScope).get.backend.$
          case None => Dummy.ref(wrapperComponentScope).get.backend.$
        }
        props.inputElementRef(implComponentScope)
      }
    }
    override def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type ThisRefComp = RefComp[Props.any, State, Backend, _ <: TopNode]
  private type State = Unit

  private case class Props[DelegateRef <: InputBase.Reference](inputElementRef: DelegateRef,
                                                               defaultValueProxy: Option[() => InputBase.Proxy],
                                                               inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement)
  private object Props {
    type any = Props[_ <: InputBase.Reference]
  }

  private final class Backend(val $: BackendScope[Props.any, State]) {
    def render(props: Props.any, state: State) = logExceptions {
      props.defaultValueProxy match {
        case Some(_) => Impl.component.withRef(Impl.ref)(props)
        case None => Dummy.component.withRef(Dummy.ref)(props)
      }
    }
  }

  private object Impl {

    val component = ReactComponentB[Props.any]("InputWithDefaultFromReferenceImpl")
      .initialState[State](ConnectionState.connectedToDefault)
      .renderBackend[Backend]
      .componentDidMount(scope => scope.backend.didMount(scope.props))
      .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
      .build
    val ref = Ref.to(component, "delegate")

    type State = ConnectionState

    final class Backend(val $: BackendScope[Props.any, State]) {
      private var currentInputValue = ""
      private var currentDefaultValue = ""

      def didMount(props: Props.any): Callback = LogExceptionsCallback {
        props.inputElementRef($).registerListener(InputValueListener)
        props.defaultValueProxy.get().registerListener(DefaultValueListener)
        currentInputValue = props.inputElementRef($).value
        currentDefaultValue = props.defaultValueProxy.get().value
      }

      def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
        props.inputElementRef($).deregisterListener(InputValueListener)
        props.defaultValueProxy.get().deregisterListener(DefaultValueListener)
      }

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference](props: Props[DelegateRef]) = {
          val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
          props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses))
        }
        renderInternal(props)
      }

      private object InputValueListener extends InputBase.Listener {
        override def onChange(newInputValue: String, directUserChange: Boolean) = LogExceptionsCallback {
          currentInputValue = newInputValue

          $.setState(ConnectionState(isConnected = currentDefaultValue == newInputValue)).runNow()
        }
      }

      private object DefaultValueListener extends InputBase.Listener {
        override def onChange(newDefaultValue: String, directUserChange: Boolean) = LogExceptionsCallback {
          currentDefaultValue = newDefaultValue

          val inputProxy = $.props.runNow().inputElementRef($)
          if ($.state.runNow().isConnected) {
            currentInputValue = inputProxy.setValue(newDefaultValue)
          }
          $.setState(ConnectionState(isConnected = newDefaultValue == currentInputValue)).runNow()
        }
      }
    }

    case class ConnectionState(isConnected: Boolean)
    object ConnectionState {
      val connectedToDefault = ConnectionState(true)
      val disconnected = ConnectionState(true)
    }
  }

  private object Dummy {

    val component = ReactComponentB[Props.any]("DummyInputWithDefaultFromReference")
      .renderBackend[Backend]
      .build
    val ref = Ref.to(component, "delegate")

    type State = Unit

    final class Backend(val $: BackendScope[Props.any, State]) {
      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference](props: Props[DelegateRef]) = {
          props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses = Seq()))
        }
        renderInternal(props)
      }
    }
  }
}

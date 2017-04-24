package flux.react.app.transactiongroupform

import common.CollectionUtils.toListMap
import common.GuavaReplacement.Iterables.getOnlyElement
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.InputBase
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag
import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import flux.react.uielements.InputBase
import japgolly.scalajs.react.{TopNode, _}

import scala.collection.immutable.Seq

private[transactiongroupform] class InputWithDefaultFromReference[Value] private() {

  private val component = ReactComponentB[Props.any]("InputWithDefaultFromReferenceWrapper")
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply[DelegateRef <: InputBase.Reference[Value]](ref: Reference,
                                                       defaultValueProxy: Option[() => InputBase.Proxy[Value]],
                                                       nameToDelegateRef: String => DelegateRef
                                                      )(inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement): ReactElement = {
    component.withRef(ref.name)(Props(
      inputElementRef = nameToDelegateRef("delegate"),
      defaultValueProxy = defaultValueProxy,
      inputElementFactory = inputElementFactory))
  }

  def apply[DelegateRef <: InputBase.Reference[Value]](ref: Reference,
                                                       defaultValueProxy: => InputBase.Proxy[Value],
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
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference[Value]](ref: DelegateRef,
                                                                               inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference](refComp: ThisRefComp) extends InputBase.Reference[Value] {
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

  private case class Props[DelegateRef <: InputBase.Reference[Value]](inputElementRef: DelegateRef,
                                                                      defaultValueProxy: Option[() => InputBase.Proxy[Value]],
                                                                      inputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement)
  private object Props {
    type any = Props[_ <: InputBase.Reference[Value]]
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
      private var currentInputValue: Value = null.asInstanceOf[Value]
      private var currentDefaultValue: Value = null.asInstanceOf[Value]

      def didMount(props: Props.any): Callback = LogExceptionsCallback {
        props.inputElementRef($).registerListener(InputValueListener)
        props.defaultValueProxy.get().registerListener(DefaultValueListener)
        currentInputValue = props.inputElementRef($).valueOrDefault
        currentDefaultValue = props.defaultValueProxy.get().valueOrDefault
      }

      def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
        props.inputElementRef($).deregisterListener(InputValueListener)
        props.defaultValueProxy.get().deregisterListener(DefaultValueListener)
      }

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
          props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses))
        }
        renderInternal(props)
      }

      private object InputValueListener extends InputBase.Listener[Value] {
        override def onChange(newInputValue: Value, directUserChange: Boolean) = LogExceptionsCallback {
          currentInputValue = newInputValue

          $.setState(ConnectionState(isConnected = currentDefaultValue == newInputValue)).runNow()
        }
      }

      private object DefaultValueListener extends InputBase.Listener[Value] {
        override def onChange(newDefaultValue: Value, directUserChange: Boolean) = LogExceptionsCallback {
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
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses = Seq()))
        }
        renderInternal(props)
      }
    }
  }
}

private[transactiongroupform] object InputWithDefaultFromReference {
  private val typeToInstance: mutable.Map[Class[_], InputWithDefaultFromReference[_]] = mutable.Map()

  def forType[Value: ClassTag]: InputWithDefaultFromReference[Value] = {
    val clazz = implicitly[ClassTag[Value]].runtimeClass
    if (!(typeToInstance contains clazz)) {
      typeToInstance.put(clazz, new InputWithDefaultFromReference[Value]())
    }
    typeToInstance(clazz).asInstanceOf[InputWithDefaultFromReference[Value]]
  }
}

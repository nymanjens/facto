package flux.react.uielements

import japgolly.scalajs.react.component.Scala.MutableRef
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag

class InputWithDefaultFromReference[Value] private () {

  private val component = ScalaComponent.builder[Props.any]("InputWithDefaultFromReferenceWrapper")
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def forOption[DelegateRef <: InputBase.Reference[Value]](
      ref: Reference,
      defaultValueProxy: Option[() => InputBase.Proxy[Value]],
      startWithDefault: Boolean = false,
      directUserChangeOnly: Boolean = false,
      nameToDelegateRef: String => DelegateRef)(
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement): VdomElement = {
    ref.mutableRef.component(
      Props(
        inputElementRef = nameToDelegateRef(ref.name),
        defaultValueProxy = defaultValueProxy,
        startWithDefault = startWithDefault,
        inputElementFactory = inputElementFactory,
        directUserChangeOnly = directUserChangeOnly
      ))
  }

  def apply[DelegateRef <: InputBase.Reference[Value]](ref: Reference,
                                                       defaultValueProxy: => InputBase.Proxy[Value],
                                                       startWithDefault: Boolean = false,
                                                       directUserChangeOnly: Boolean = false,
                                                       nameToDelegateRef: String => DelegateRef)(
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement): VdomElement = {
    forOption(
      ref = ref,
      defaultValueProxy = Some(() => defaultValueProxy),
      startWithDefault = startWithDefault,
      nameToDelegateRef = nameToDelegateRef,
      directUserChangeOnly = directUserChangeOnly
    )(inputElementFactory)
  }

  def ref(name: String): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference[Value]](ref: DelegateRef,
                                                                               inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference] (mutableRef: ThisMutableRef)
      extends InputBase.Reference[Value] {
    override def apply($ : BackendScope[_, _]) = {
      InputBase.Proxy.forwardingTo {
        val component = mutableRef($).get
        val wrapperComponentScope = component.backend.$
        val props = component.props

        val implComponentScope = props.defaultValueProxy match {
          case Some(_) => Impl.ref(wrapperComponentScope).get.backend.$
          case None => Dummy.ref(wrapperComponentScope).get.backend.$
        }
        props.inputElementRef(implComponentScope)
      }
    }
    override def name = mutableRef.name
  }

  // **************** Private inner types ****************//
  private type ThisMutableRef = MutableRef[Props.any, State, Backend, _]
  private type State = Unit

  private case class Props[DelegateRef <: InputBase.Reference[Value]](
      inputElementRef: DelegateRef,
      defaultValueProxy: Option[() => InputBase.Proxy[Value]],
      startWithDefault: Boolean,
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement,
      directUserChangeOnly: Boolean)
  private object Props {
    type any = Props[_ <: InputBase.Reference[Value]]
  }

  private final class Backend(val $ : BackendScope[Props.any, State]) {
    def render(props: Props.any, state: State) = logExceptions {
      props.defaultValueProxy match {
        case Some(_) => Impl.component.withRef(Impl.ref)(props)
        case None => Dummy.component.withRef(Dummy.ref)(props)
      }
    }
  }

  private object Impl {

    val component = ScalaComponent.builder[Props.any]("InputWithDefaultFromReferenceImpl")
      .initialState[State](ConnectionState.connectedToDefault)
      .renderBackend[Backend]
      .componentDidMount(scope => scope.backend.didMount(scope.props))
      .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
      .build
    val ref = Ref.to(component, "delegate")

    type State = ConnectionState

    final class Backend(val $ : BackendScope[Props.any, State]) {
      private var currentInputValue: Value = null.asInstanceOf[Value]
      private var currentDefaultValue: Value = null.asInstanceOf[Value]

      def didMount(props: Props.any): Callback = LogExceptionsCallback {
        props.inputElementRef($).registerListener(InputValueListener)
        props.defaultValueProxy.get().registerListener(DefaultValueListener)

        currentDefaultValue = props.defaultValueProxy.get().valueOrDefault
        if (props.startWithDefault) {
          currentInputValue = currentDefaultValue
          props.inputElementRef($).setValue(currentDefaultValue)
        } else {
          currentInputValue = props.inputElementRef($).valueOrDefault
          $.setState(ConnectionState(isConnected = currentDefaultValue == currentInputValue)).runNow()
        }
      }

      def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
        try {
          props.defaultValueProxy.get().deregisterListener(DefaultValueListener)
        } catch {
          case _: NoSuchElementException => // This is OK because the default value input is probably already unmounted.
        }
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
          if ($.state
                .runNow()
                .isConnected && (directUserChange || ! $.props.runNow().directUserChangeOnly)) {
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

    val component = ScalaComponent.builder[Props.any]("DummyInputWithDefaultFromReference")
      .renderBackend[Backend]
      .build
    val ref = Ref.to(component, "delegate")

    type State = Unit

    final class Backend(val $ : BackendScope[Props.any, State]) {
      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          props.inputElementFactory(InputElementExtraProps(props.inputElementRef, inputClasses = Seq()))
        }
        renderInternal(props)
      }
    }
  }
}

object InputWithDefaultFromReference {
  private val typeToInstance: mutable.Map[Class[_], InputWithDefaultFromReference[_]] = mutable.Map()

  def forType[Value: ClassTag]: InputWithDefaultFromReference[Value] = {
    val clazz = implicitly[ClassTag[Value]].runtimeClass
    if (!(typeToInstance contains clazz)) {
      typeToInstance.put(clazz, new InputWithDefaultFromReference[Value]())
    }
    typeToInstance(clazz).asInstanceOf[InputWithDefaultFromReference[Value]]
  }
}

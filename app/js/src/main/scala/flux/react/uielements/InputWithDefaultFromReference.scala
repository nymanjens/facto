package flux.react.uielements

import japgolly.scalajs.react.component.Scala.MutableRef
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import japgolly.scalajs.react._
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag

class InputWithDefaultFromReference[Value] private () {

  private val component = ScalaComponent
    .builder[Props.any]("InputWithDefaultFromReferenceWrapper")
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def forOption[DelegateRef <: InputBase.Reference[Value]](
      ref: Reference,
      defaultValueProxy: Option[() => InputBase.Proxy[Value]],
      startWithDefault: Boolean = false,
      directUserChangeOnly: Boolean = false,
      delegateRefFactory: () => DelegateRef)(
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement): VdomElement = {
    ref.mutableRef
      .component(
        Props(
          delegateRefFactory = delegateRefFactory,
          defaultValueProxy = defaultValueProxy,
          startWithDefault = startWithDefault,
          inputElementFactory = inputElementFactory,
          directUserChangeOnly = directUserChangeOnly
        ))
      .vdomElement
  }

  def apply[DelegateRef <: InputBase.Reference[Value]](ref: Reference,
                                                       defaultValueProxy: => InputBase.Proxy[Value],
                                                       startWithDefault: Boolean = false,
                                                       directUserChangeOnly: Boolean = false,
                                                       delegateRefFactory: () => DelegateRef)(
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement): VdomElement = {
    forOption(
      ref = ref,
      defaultValueProxy = Some(() => defaultValueProxy),
      startWithDefault = startWithDefault,
      delegateRefFactory = delegateRefFactory,
      directUserChangeOnly = directUserChangeOnly
    )(inputElementFactory)
  }

  def ref(): Reference = new Reference(ScalaComponent.mutableRefTo(component))

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference[Value]](ref: DelegateRef,
                                                                               inputClasses: Seq[String])

  final class Reference private[InputWithDefaultFromReference] (
      private[InputWithDefaultFromReference] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[Value] {
    override def apply() = {
      InputBase.Proxy.forwardingTo {
        val backend = mutableRef.value.backend
        mutableRef.value.props.defaultValueProxy match {
          case Some(_) => backend.implRef.value.backend.inputElementRef()
          case None => backend.dummyRef.value.backend.inputElementRef()
        }
      }
    }
  }

  // **************** Private inner types ****************//
  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props.any], Children.None, CtorType.Props]
  private type ThisMutableRef = MutableRef[Props.any, State, Backend, ThisCtorSummoner#CT]
  private type State = Unit

  private case class Props[DelegateRef <: InputBase.Reference[Value]](
      delegateRefFactory: () => DelegateRef,
      defaultValueProxy: Option[() => InputBase.Proxy[Value]],
      startWithDefault: Boolean,
      inputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement,
      directUserChangeOnly: Boolean)
  private object Props {
    type any = Props[_ <: InputBase.Reference[Value]]
  }

  private final class Backend(val $ : BackendScope[Props.any, State]) {
    val implRef = ScalaComponent.mutableRefTo(Impl.component)
    val dummyRef = ScalaComponent.mutableRefTo(Dummy.component)

    def render(props: Props.any, state: State) = logExceptions {
      props.defaultValueProxy match {
        case Some(_) => implRef.component(props).vdomElement
        case None => dummyRef.component(props).vdomElement
      }
    }
  }

  private object Impl {

    val component = ScalaComponent
      .builder[Props.any]("InputWithDefaultFromReferenceImpl")
      .initialState[State](ConnectionState.connectedToDefault)
      .renderBackend[Backend]
      .componentDidMount(scope => scope.backend.didMount(scope.props))
      .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
      .build

    type State = ConnectionState

    final class Backend(val $ : BackendScope[Props.any, State]) {
      private var currentInputValue: Value = null.asInstanceOf[Value]
      private var currentDefaultValue: Value = null.asInstanceOf[Value]
      private[InputWithDefaultFromReference] lazy val inputElementRef = $.props.runNow().delegateRefFactory()

      def didMount(props: Props.any): Callback = LogExceptionsCallback {
        inputElementRef().registerListener(InputValueListener)
        props.defaultValueProxy.get().registerListener(DefaultValueListener)

        currentDefaultValue = props.defaultValueProxy.get().valueOrDefault
        if (props.startWithDefault) {
          currentInputValue = currentDefaultValue
          inputElementRef().setValue(currentDefaultValue)
        } else {
          currentInputValue = inputElementRef().valueOrDefault
          $.setState(ConnectionState(isConnected = currentDefaultValue == currentInputValue)).runNow()
        }
      }

      def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
        try {
          props.defaultValueProxy.get().deregisterListener(DefaultValueListener)
        } catch {
          case e: Throwable => // TODO: Make this more narrow
            println("!!!!!!!!!!!!!! Ignoring exception: " + e.getMessage)
        }
      }

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
          props.inputElementFactory(
            InputElementExtraProps(inputElementRef.asInstanceOf[DelegateRef], inputClasses))
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

          val inputProxy = inputElementRef()
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

    val component = ScalaComponent
      .builder[Props.any]("DummyInputWithDefaultFromReference")
      .renderBackend[Backend]
      .build

    type State = Unit

    final class Backend(val $ : BackendScope[Props.any, State]) {
      private[InputWithDefaultFromReference] lazy val inputElementRef = $.props.runNow().delegateRefFactory()

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          props.inputElementFactory(
            InputElementExtraProps(inputElementRef.asInstanceOf[DelegateRef], inputClasses = Seq()))
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

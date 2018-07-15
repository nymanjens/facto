package flux.react.uielements.input

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MutableRef
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Wrapper component around a given input component that provides a dynamic default value for
  * that component.
  */
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
      Option(mutableRef.value) flatMap { proxy =>
        val backend = proxy.backend
        proxy.props.defaultValueProxy match {
          case Some(_) => Option(backend.implRef.value) map (_.backend.delegateRef())
          case None    => Option(backend.dummyRef.value) map (_.backend.delegateRef())
        }
      } getOrElse InputBase.Proxy.nullObject()
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
        case None    => dummyRef.component(props).vdomElement
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
      private[InputWithDefaultFromReference] lazy val delegateRef = $.props.runNow().delegateRefFactory()

      def didMount(props: Props.any): Callback = LogExceptionsCallback {
        delegateRef().registerListener(InputValueListener)
        props.defaultValueProxy.get().registerListener(DefaultValueListener)

        currentDefaultValue = props.defaultValueProxy.get().valueOrDefault
        if (props.startWithDefault) {
          currentInputValue = delegateRef().setValue(currentDefaultValue)
          $.setState(ConnectionState(isConnected = currentDefaultValue == currentInputValue)).runNow()
          val callSucceeded = currentInputValue == currentDefaultValue

          if (!callSucceeded) {
            // Try again at a later time. This can happen if there are inputs dependent on each others value
            Future {
              currentInputValue = delegateRef().setValue(currentDefaultValue)
              $.setState(ConnectionState(isConnected = currentDefaultValue == currentInputValue)).runNow()
            }
          }
        } else {
          currentInputValue = delegateRef().valueOrDefault
          $.setState(ConnectionState(isConnected = currentDefaultValue == currentInputValue)).runNow()
        }
      }

      def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
        props.defaultValueProxy.get().deregisterListener(DefaultValueListener)
      }

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          val inputClasses = if (state.isConnected) Seq("bound-until-change") else Seq()
          props.inputElementFactory(
            InputElementExtraProps(delegateRef.asInstanceOf[DelegateRef], inputClasses))
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

          val inputProxy = delegateRef()
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
      private[InputWithDefaultFromReference] lazy val delegateRef = $.props.runNow().delegateRefFactory()

      def render(props: Props.any, state: State) = logExceptions {
        def renderInternal[DelegateRef <: InputBase.Reference[Value]](props: Props[DelegateRef]) = {
          props.inputElementFactory(
            InputElementExtraProps(delegateRef.asInstanceOf[DelegateRef], inputClasses = Seq()))
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

package app.flux.react.uielements.input

import app.flux.react.uielements.input.MappedInput.ValueTransformer
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.LocalDateTime
import hydro.common.time.TimeUtils
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.input.InputValidator
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom._

import scala.collection.mutable
import scala.reflect.ClassTag

class MappedInput[DelegateValue, Value] private (implicit
    delegateValueTag: ClassTag[DelegateValue],
    valueTag: ClassTag[Value],
) {

  private val component = ScalaComponent
    .builder[Props.any](
      s"${getClass.getSimpleName}_${delegateValueTag.runtimeClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}"
    )
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply[DelegateRef <: InputBase.Reference[DelegateValue]](
      ref: Reference,
      defaultValue: Value,
      valueTransformer: ValueTransformer[DelegateValue, Value],
      listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance,
      delegateRefFactory: () => DelegateRef,
  )(delegateInputElementFactory: InputElementExtraProps[DelegateRef] => VdomElement): VdomElement = {
    ref.mutableRef
      .component(
        Props(
          delegateRefFactory = delegateRefFactory,
          valueTransformer,
          defaultValue,
          listener,
          delegateElementFactory = delegateInputElementFactory,
        )
      )
      .vdomElement
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))
  def delegateRef(ref: Reference): DelegateReference = new DelegateReference(ref.mutableRef)

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference[DelegateValue]](
      ref: DelegateRef,
      defaultValue: DelegateValue,
      additionalValidator: InputValidator[DelegateValue],
  )

  final class Reference private[MappedInput] (private[MappedInput] val mutableRef: ThisMutableRef)
      extends InputBase.Reference[Value] {
    override def apply(): InputBase.Proxy[Value] = {
      mutableRef.get.asCallback.runNow() map (new Proxy(_)) getOrElse InputBase.Proxy.nullObject()
    }
  }

  final class DelegateReference private[MappedInput] (mutableRef: ThisMutableRef)
      extends InputBase.Reference[DelegateValue] {
    override def apply(): InputBase.Proxy[DelegateValue] = {
      mutableRef.get.asCallback.runNow() map (_.backend.delegateRef()) getOrElse InputBase.Proxy.nullObject()
    }
  }

  // **************** Private inner types ****************//
  private type State = Unit
  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props.any], Children.None, CtorType.Props]
  private type ThisMutableRef = ToScalaComponent[Props.any, State, Backend, ThisCtorSummoner#CT]
  private type ThisComponentU = MountedImpure[Props.any, State, Backend]

  private final class Proxy(private val component: ThisComponentU) extends InputBase.Proxy[Value] {
    override def value = delegateProxy.value flatMap props.valueTransformer.forward
    override def valueOrDefault =
      props.valueTransformer.forward(delegateProxy.valueOrDefault) getOrElse props.defaultValue
    override def setValue(newValue: Value) = {
      val result = delegateProxy.setValue(props.valueTransformer.backward(newValue))
      props.valueTransformer.forward(result) getOrElse props.defaultValue
    }
    override def registerListener(listener: InputBase.Listener[Value]) = {
      delegateProxy.registerListener(Proxy.toDelegateListener(listener, props))
    }
    override def deregisterListener(listener: InputBase.Listener[Value]) = {
      delegateProxy.deregisterListener(Proxy.toDelegateListener(listener, props))
    }

    private def props: Props.any = component.props
    private def delegateProxy: InputBase.Proxy[DelegateValue] = {
      component.backend.delegateRef()
    }
  }

  private object Proxy {
    private val mappedToDelegateListener
        : mutable.Map[InputBase.Listener[Value], InputBase.Listener[DelegateValue]] = mutable.Map()

    def toDelegateListener(
        mappedListener: InputBase.Listener[Value],
        props: Props.any,
    ): InputBase.Listener[DelegateValue] = {
      if (!(mappedToDelegateListener contains mappedListener)) {
        mappedToDelegateListener.put(
          mappedListener,
          new InputBase.Listener[DelegateValue] {
            override def onChange(newDelegateValue: DelegateValue, directUserChange: Boolean) =
              LogExceptionsCallback {
                mappedListener
                  .onChange(
                    newValue = props.valueTransformer.forward(newDelegateValue) getOrElse props.defaultValue,
                    directUserChange = directUserChange,
                  )
                  .runNow()
              }
          },
        )
      }
      mappedToDelegateListener(mappedListener)
    }
  }

  private case class Props[DelegateRef <: InputBase.Reference[DelegateValue]](
      delegateRefFactory: () => DelegateRef,
      valueTransformer: ValueTransformer[DelegateValue, Value],
      defaultValue: Value,
      listener: InputBase.Listener[Value],
      delegateElementFactory: InputElementExtraProps[DelegateRef] => VdomElement,
  )
  private object Props {
    type any = Props[_ <: InputBase.Reference[DelegateValue]]
  }

  private final class Backend(val $ : BackendScope[Props.any, State]) {
    private[MappedInput] lazy val delegateRef: InputBase.Reference[DelegateValue] =
      $.props.runNow().delegateRefFactory()

    def didMount(props: Props.any): Callback = LogExceptionsCallback {
      delegateRef().registerListener(Proxy.toDelegateListener(props.listener, props))
    }

    def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
      delegateRef().deregisterListener(Proxy.toDelegateListener(props.listener, props))
    }

    def render(props: Props.any, state: State) = logExceptions {
      def renderInternal[DelegateRef <: InputBase.Reference[DelegateValue]](props: Props[DelegateRef]) = {
        val defaultDelegateValue = props.valueTransformer.backward(props.defaultValue)
        props.delegateElementFactory(
          InputElementExtraProps(
            ref = delegateRef.asInstanceOf[DelegateRef],
            defaultValue = defaultDelegateValue,
            additionalValidator = value => props.valueTransformer.forward(value).isDefined,
          )
        )
      }
      renderInternal(props)
    }
  }
}

object MappedInput {
  private val typesToInstance: mutable.Map[(Class[_], Class[_]), MappedInput[_, _]] = mutable.Map()

  def forTypes[DelegateValue: ClassTag, Value: ClassTag]: MappedInput[DelegateValue, Value] = {
    val classes: (Class[_], Class[_]) =
      (implicitly[ClassTag[DelegateValue]].runtimeClass, implicitly[ClassTag[Value]].runtimeClass)
    if (!(typesToInstance contains classes)) {
      typesToInstance.put(classes, new MappedInput[DelegateValue, Value]())
    }
    typesToInstance(classes).asInstanceOf[MappedInput[DelegateValue, Value]]
  }

  trait ValueTransformer[DelegateValue, Value] {

    /**
      * Returns the Value that corresponds to the given DelegateValue or None iff the DelegateValue is
      * invalid.
      */
    def forward(delegateValue: DelegateValue): Option[Value]

    /** Returns the DelegateValue corresponding to the given value. */
    def backward(value: Value): DelegateValue
  }

  object ValueTransformer {
    object StringToLocalDateTime extends ValueTransformer[String, LocalDateTime] {
      override def forward(string: String) = {
        try {
          Some(TimeUtils.parseDateString(string.trim))
        } catch {
          case _: IllegalArgumentException => None
        }
      }
      override def backward(value: LocalDateTime) = value.toLocalDate.toString
    }
  }
}

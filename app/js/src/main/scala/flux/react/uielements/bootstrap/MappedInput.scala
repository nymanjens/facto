package flux.react.uielements.bootstrap

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.uielements.InputBase
import flux.react.uielements.bootstrap.InputComponent.ValueTransformer
import japgolly.scalajs.react.{TopNode, _}

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.reflect.ClassTag

class MappedInput[DelegateValue, Value] private(implicit delegateValueTag: ClassTag[DelegateValue],
                                                                              valueTag: ClassTag[Value]) {

  private val component = ReactComponentB[Props](
    s"${getClass.getSimpleName}_${delegateValueTag.runtimeClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply(ref: Reference,
            delegateRef: InputBase.Reference[DelegateValue],
            defaultValue: Value,
            valueTransformer: ValueTransformer,
            listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance
           )(delegateInputElement: => ReactElement): ReactElement = {
    component.withRef(ref.name)(Props(
      delegateRef = delegateRef,
      valueTransformer,
      defaultValue,
      listener,
      delegateElementFactory = () => delegateInputElement))
  }


  def ref(name: String): Reference = new Reference(Ref.to(component, name))

  // **************** Public inner types ****************//
  trait ValueTransformer {

    /**
      * Returns the Value that corresponds to the given DelegateValue or None iff the DelegateValue is
      * invalid.
      */
    def forward(delegateValue: DelegateValue): Option[Value]

    /** Returns the DelegateValue corresponding to the given value. */
    def backward(value: Value): DelegateValue
  }

  final class Reference private[MappedInput](refComp: ThisRefComp) extends InputBase.Reference[Value] {
    override def apply($: BackendScope[_, _]): InputBase.Proxy[Value] = new Proxy(() => refComp($).get)
    override def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type State = Unit
  private type ThisRefComp = RefComp[Props, State, Backend, _ <: TopNode]
  private type ThisComponentU = ReactComponentU[Props, State, Backend, _ <: TopNode]

  private final class Proxy(val componentProvider: () => ThisComponentU) extends InputBase.Proxy[Value] {
    override def value = delegateProxy.value flatMap props.valueTransformer.forward
    override def valueOrDefault = props.valueTransformer.forward(delegateProxy.valueOrDefault) getOrElse props.defaultValue
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

    private def props: Props = componentProvider().props
    private def delegateProxy: InputBase.Proxy[DelegateValue] = {
      val context = componentProvider().backend.$
      props.delegateRef(context)
    }
  }

  private object Proxy {
    private val mappedToDelegateListener: mutable.Map[InputBase.Listener[Value], InputBase.Listener[DelegateValue]] = mutable.Map()

    def toDelegateListener(mappedListener: InputBase.Listener[Value], props: Props): InputBase.Listener[DelegateValue] = {
      if (!(mappedToDelegateListener contains mappedListener)) {
        mappedToDelegateListener.put(mappedListener, new InputBase.Listener[DelegateValue] {
          override def onChange(newDelegateValue: DelegateValue, directUserChange: Boolean) = LogExceptionsCallback {
            mappedListener.onChange(
              newValue = props.valueTransformer.forward(newDelegateValue) getOrElse props.defaultValue,
              directUserChange = directUserChange).runNow()
          }
        })
      }
      mappedToDelegateListener(mappedListener)
    }
  }

  private case class Props(delegateRef: InputBase.Reference[DelegateValue],
                           valueTransformer: ValueTransformer,
                           defaultValue: Value,
                           listener: InputBase.Listener[Value],
                           delegateElementFactory: () => ReactElement)

  private final class Backend(val $: BackendScope[Props, State]) {
    def didMount(props: Props): Callback = LogExceptionsCallback {
      props.delegateRef($).registerListener(Proxy.toDelegateListener(props.listener, props))
    }

    def willUnmount(props: Props): Callback = LogExceptionsCallback {
      props.delegateRef($).deregisterListener(Proxy.toDelegateListener(props.listener, props))
    }

    def render(props: Props, state: State) = logExceptions {
      props.delegateElementFactory()
    }
  }
}

object MappedInput {
  private val typesToInstance: mutable.Map[(Class[_], Class[_]), MappedInput[_, _]] = mutable.Map()

  def forTypes[DelegateValue: ClassTag, Value: ClassTag]: MappedInput[DelegateValue, Value] = {
    val classes = (implicitly[ClassTag[DelegateValue]].runtimeClass, implicitly[ClassTag[Value]].runtimeClass)
    if (!(typesToInstance contains classes)) {
      typesToInstance.put(classes, new MappedInput[DelegateValue, Value]())
    }
    typesToInstance(classes).asInstanceOf[MappedInput[DelegateValue, Value]]
  }
}

package flux.react.uielements

import java.time.LocalDate

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.time.TimeUtils
import flux.react.uielements.MappedInput.ValueTransformer
import japgolly.scalajs.react.{TopNode, _}

import scala.collection.mutable
import scala.reflect.ClassTag

class MappedInput[DelegateValue, Value] private(implicit delegateValueTag: ClassTag[DelegateValue],
                                                valueTag: ClassTag[Value]) {

  private val component = ReactComponentB[Props.any](
    s"${getClass.getSimpleName}_${delegateValueTag.runtimeClass.getSimpleName}_${valueTag.runtimeClass.getSimpleName}")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply[DelegateRef <: InputBase.Reference[DelegateValue]](ref: Reference,
                                                               defaultValue: Value,
                                                               valueTransformer: ValueTransformer[DelegateValue, Value],
                                                               listener: InputBase.Listener[Value] = InputBase.Listener.nullInstance,
                                                               nameToDelegateRef: String => DelegateRef
                                                              )(delegateInputElementFactory: InputElementExtraProps[DelegateRef] => ReactElement): ReactElement = {
    component.withRef(ref.name)(Props(
      delegateRef = nameToDelegateRef("delegate"),
      valueTransformer,
      defaultValue,
      listener,
      delegateElementFactory = delegateInputElementFactory))
  }


  def ref(name: String): Reference = new Reference(Ref.to(component, name))
  def delegateRef(ref: Reference): DelegateReference = new DelegateReference(Ref.to(component, ref.name))

  // **************** Public inner types ****************//
  case class InputElementExtraProps[DelegateRef <: InputBase.Reference[DelegateValue]](ref: DelegateRef,
                                                                                       defaultValue: DelegateValue)

  final class Reference private[MappedInput](refComp: ThisRefComp) extends InputBase.Reference[Value] {
    override def apply($: BackendScope[_, _]): InputBase.Proxy[Value] = new Proxy(() => refComp($).get)
    override def name = refComp.name
  }

  final class DelegateReference private[MappedInput](refComp: ThisRefComp) extends InputBase.Reference[DelegateValue] {
    override def apply($: BackendScope[_, _]): InputBase.Proxy[DelegateValue] = {
      val component = refComp($).get
      component.props.delegateRef(component.backend.$)
    }
    override def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type State = Unit
  private type ThisRefComp = RefComp[Props.any, State, Backend, _ <: TopNode]
  private type ThisComponentU = ReactComponentU[Props.any, State, Backend, _ <: TopNode]

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

    private def props: Props.any = componentProvider().props
    private def delegateProxy: InputBase.Proxy[DelegateValue] = {
      val context = componentProvider().backend.$
      props.delegateRef(context)
    }
  }

  private object Proxy {
    private val mappedToDelegateListener: mutable.Map[InputBase.Listener[Value], InputBase.Listener[DelegateValue]] = mutable.Map()

    def toDelegateListener(mappedListener: InputBase.Listener[Value], props: Props.any): InputBase.Listener[DelegateValue] = {
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

  private case class Props[DelegateRef <: InputBase.Reference[DelegateValue]](delegateRef: DelegateRef,
                                                                              valueTransformer: ValueTransformer[DelegateValue, Value],
                                                                              defaultValue: Value,
                                                                              listener: InputBase.Listener[Value],
                                                                              delegateElementFactory: InputElementExtraProps[DelegateRef] => ReactElement)
  private object Props {
    type any = Props[_ <: InputBase.Reference[DelegateValue]]
  }

  private final class Backend(val $: BackendScope[Props.any, State]) {
    def didMount(props: Props.any): Callback = LogExceptionsCallback {
      props.delegateRef($).registerListener(Proxy.toDelegateListener(props.listener, props))
    }

    def willUnmount(props: Props.any): Callback = LogExceptionsCallback {
      props.delegateRef($).deregisterListener(Proxy.toDelegateListener(props.listener, props))
    }

    def render(props: Props.any, state: State) = logExceptions {
      def renderInternal[DelegateRef <: InputBase.Reference[DelegateValue]](props: Props[DelegateRef]) = {
        val defaultDelegateValue = props.valueTransformer.backward(props.defaultValue)
        props.delegateElementFactory(InputElementExtraProps(props.delegateRef, defaultDelegateValue))
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
    object StringToLocalDate extends ValueTransformer[String, LocalDate] {
      override def forward(string: String) = {
        try {
          Some(TimeUtils.parseDateString(string).toLocalDate)
        } catch {
          case _: IllegalArgumentException => None
        }
      }
      override def backward(value: LocalDate) = value.toString
    }
  }
}

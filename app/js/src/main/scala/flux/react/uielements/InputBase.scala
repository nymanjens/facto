package flux.react.uielements

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

object InputBase {

  trait Reference[Value] {
    def apply(): Proxy[Value]
  }

  trait Proxy[Value] {

    /** Returns None if this field is invalidly formatted. */
    def value: Option[Value]

    def valueOrDefault: Value

    /**
      * Returns the value after this change. This may be different from the input if the input is
      * invalid for this field. May return the default value if there is no valid value to return.
      */
    def setValue(value: Value): Value

    final def valueIsValid: Boolean = value.isDefined

    def registerListener(listener: Listener[Value]): Unit
    def deregisterListener(listener: Listener[Value]): Unit
  }

  object Proxy {
    def nullObject[Value](): Proxy[Value] = new NullObject

    private final class NullObject[Value]() extends Proxy[Value] {
      override def value = None
      override def valueOrDefault = null.asInstanceOf[Value]
      override def setValue(value: Value) = value
      override def registerListener(listener: Listener[Value]) = {}
      override def deregisterListener(listener: Listener[Value]) = {}
    }
  }

  trait Listener[-Value] {

    /** Gets called every time this field gets updated. This includes updates that are not done by the user. */
    def onChange(newValue: Value, directUserChange: Boolean): Callback
  }

  object Listener {
    def nullInstance[Value] = new Listener[Value] {
      override def onChange(newValue: Value, directUserChange: Boolean) = Callback.empty
    }
  }
}

package hydro.flux.react.uielements.input

import japgolly.scalajs.react._
import org.scalajs.dom.console

/**
  * Contains base traits to be used by input components that describe a single value.
  *
  * Aggregate input values are not supported.
  */
object InputBase {

  /** A reference to an input component. */
  trait Reference[Value] {
    def apply(): Proxy[Value]
  }

  /** Proxy that allows client code of an input component to interact with its value. */
  trait Proxy[Value] {

    /** Returns the current value or None if this field is invalidly formatted. */
    def value: Option[Value]

    /** Returns the current value or the default if this field is invalidly formatted. */
    def valueOrDefault: Value

    /**
      * Sets the value of the input component and returns the value after this change.
      *
      * The return value may be different from the input if the input is invalid for this
      * field.
      */
    def setValue(value: Value): Value

    final def valueIsValid: Boolean = value.isDefined

    /** Focuses the input field. */
    def focus(): Unit = {
      console.log("focus() not implemented")
      throw new UnsupportedOperationException()
    }

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

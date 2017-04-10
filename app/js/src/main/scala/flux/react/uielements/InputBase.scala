package flux.react.uielements

import japgolly.scalajs.react._

object InputBase {

  trait Reference {
    def apply($: BackendScope[_, _]): Proxy
    def name: String
  }

  trait Proxy {
    def value: String
    /**
      * Returns the value after this change. This may be different from the input if the input is
      * invalid for this field.
      */
    def setValue(string: String): String
    def registerListener(listener: Listener): Unit
    def deregisterListener(listener: Listener): Unit
  }

  object Proxy {
    def forwardingTo(delegate: => Proxy): Proxy = new ForwardingImpl(() => delegate)

    private final class ForwardingImpl(delegateProvider: () => Proxy) extends Proxy {
      override def value: String = delegateProvider().value
      override def setValue(string: String) = delegateProvider().setValue(string)
      override def registerListener(listener: Listener): Unit = delegateProvider().registerListener(listener)
      override def deregisterListener(listener: Listener): Unit = delegateProvider().deregisterListener(listener)
    }
  }

  trait Listener {
    /** Gets called every time this field gets updated. This includes updates that are not done by the user. */
    def onChange(newValue: String, directUserChange: Boolean): Callback
  }

  object Listener {
    val nullInstance = new Listener {
      override def onChange(newValue: String, directUserChange: Boolean) = Callback.empty
    }
  }
}

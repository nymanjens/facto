package flux.react.uielements

import java.util.NoSuchElementException

import common.LoggingUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{^^, <<}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement
import japgolly.scalajs.react.TopNode

import scala.collection.immutable.Seq

object InputBase {

  trait Reference {
    def apply($: BackendScope[_, _]): Proxy
    def name: String
  }

  trait Proxy {
    def value: String
    def setValue(string: String): Unit
    def registerListener(listener: Listener): Unit
    def deregisterListener(listener: Listener): Unit
  }

  object Proxy {
    def forwardingTo(delegate: => Proxy): Proxy = new ForwardingImpl(() => delegate)

    private final class ForwardingImpl(delegateProvider: () => Proxy) extends Proxy {
      override def value: String = delegateProvider().value
      override def setValue(string: String): Unit = delegateProvider().setValue(string)
      override def registerListener(listener: Listener): Unit = delegateProvider().registerListener(listener)
      override def deregisterListener(listener: Listener): Unit = delegateProvider().deregisterListener(listener)
    }
  }

  trait Listener {
    /** Gets called every time this field gets updated. This includes updates that are not done by the user. */
    def onChange(newValue: String): Callback
  }
}

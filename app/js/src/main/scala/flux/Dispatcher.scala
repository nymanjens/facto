package flux

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Dispatcher is used to broadcast payloads to registered callbacks.
  *
  * Modelled after Facebook's dispatcher: https://github.com/facebook/flux/blob/master/src/Dispatcher.js
  */
trait Dispatcher {

  def register(callback: Action => Unit): Unit

  def dispatch(action: Action): Future[Unit]
}

object Dispatcher {
  private[flux] final class Impl extends Dispatcher {
    var callbacks: Seq[Action => Unit] = Seq()
    var isDispatching: Boolean = false

    def register(callback: Action => Unit) = {
      require(!isDispatching)
      callbacks = callbacks :+ callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching)

      Future {
        require(!isDispatching)
        isDispatching = true
        callbacks.foreach(_.apply(action))
        isDispatching = false
      }
    }
  }

  final class FakeSynchronous extends Dispatcher {
    var callbacks: Seq[Action => Unit] = Seq()
    var isDispatching: Boolean = false

    def register(callback: Action => Unit) = {
      require(!isDispatching)
      callbacks = callbacks :+ callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching)

      isDispatching = true
      callbacks.foreach(_.apply(action))
      isDispatching = false

      Future.successful((): Unit)
    }
  }
}

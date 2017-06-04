package flux.action

import common.LoggingUtils.{logExceptions, LogExceptionsCallback}

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Dispatcher is used to broadcast payloads to registered callbacks.
  *
  * Modelled after Facebook's dispatcher: https://github.com/facebook/flux/blob/master/src/Dispatcher.js
  */
trait Dispatcher {

  def register(callback: Action => Unit): Unit
  final def registerPartial(callback: PartialFunction[Action, Unit]): Unit = {
    register(callback orElse Dispatcher.nullCallback)
  }

  def dispatch(action: Action): Future[Unit]
}

object Dispatcher {
  private def nullCallback: PartialFunction[Action, Unit] = {
    case _ =>
  }

  private[flux] final class Impl extends Dispatcher {
    private var callbacks: Set[Action => Unit] = Set()
    private var isDispatching: Boolean = false

    def register(callback: Action => Unit) = {
      require(!isDispatching)
      callbacks = callbacks + callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching, s"Dispatch triggered action $action")

      Future {
        logExceptions {
          require(!isDispatching)
          isDispatching = true
          println(s"  Dispatcher: Dispatching action ${action.getClass.getSimpleName}")
          callbacks.foreach(_.apply(action))
          callbacks.foreach(_.apply(Action.Done(action)))
          isDispatching = false
        }
      }
    }
  }

  final class FakeSynchronous extends Dispatcher {
    private var _callbacks: Set[Action => Unit] = Set()
    private val _dispatchedActions: mutable.Buffer[Action] = mutable.Buffer()
    private var isDispatching: Boolean = false

    // ******************* Implementation of Dispatcher interface ******************* //
    def register(callback: Action => Unit) = {
      require(!isDispatching)
      _callbacks = _callbacks + callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching)

      isDispatching = true
      _callbacks.foreach(_.apply(action))
      isDispatching = false

      _dispatchedActions += action

      Future.successful((): Unit)
    }

    // ******************* Additional API for testing ******************* //
    def dispatchedActions: Seq[Action] = _dispatchedActions.toVector
    def callbacks: Seq[Action => Unit] = _callbacks.toVector
  }
}

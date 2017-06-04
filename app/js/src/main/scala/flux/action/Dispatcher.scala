package flux.action

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import scala.concurrent.duration._
import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Dispatcher is used to broadcast payloads to registered callbacks.
  *
  * Modelled after Facebook's dispatcher: https://github.com/facebook/flux/blob/master/src/Dispatcher.js
  */
trait Dispatcher {

  def registerAsync(callback: Action => Future[Unit]): Unit
  final def registerPartialAsync(callback: PartialFunction[Action, Future[Unit]]): Unit = {
    registerAsync(callback orElse Dispatcher.nullCallback)
  }
  final def registerPartialSync(callback: PartialFunction[Action, Unit]): Unit = {
    registerPartialAsync(callback.andThen(_ => Future.successful((): Unit)))
  }

  def dispatch(action: Action): Future[Unit]
}

object Dispatcher {
  private def nullCallback: PartialFunction[Action, Future[Unit]] = {
    case _ => Future.successful((): Unit)
  }

  private[flux] final class Impl extends Dispatcher {
    private var callbacks: Set[Action => Future[Unit]] = Set()
    private var isDispatching: Boolean = false

    def registerAsync(callback: Action => Future[Unit]) = {
      require(!isDispatching)
      callbacks = callbacks + callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching, s"Dispatch triggered action $action")

      async {
        println(s"  Dispatcher: Dispatching action ${action.getClass.getSimpleName}")
        await(invokeCallbacks(action))
        println(s"  Dispatcher: Dispatching action Action.Done(${action.getClass.getSimpleName})")
        await(invokeCallbacks(Action.Done(action)))
      }
    }

    private def invokeCallbacks(action: Action): Future[Unit] = {
      val future = Future.sequence {
        logExceptions {
          isDispatching = true
          val seqOfFutures = for (callback <- callbacks) yield callback(action)
          isDispatching = false
          seqOfFutures
        }
      }
      future.map(_ => (): Unit)
    }
  }

  final class FakeSynchronous extends Dispatcher {
    private var _callbacks: Set[Action => Future[Unit]] = Set()
    private val _dispatchedActions: mutable.Buffer[Action] = mutable.Buffer()
    private var isDispatching: Boolean = false

    // ******************* Implementation of Dispatcher interface ******************* //
    def registerAsync(callback: Action => Future[Unit]) = {
      require(!isDispatching)
      _callbacks = _callbacks + callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching)

      isDispatching = true
      println(s"  Dispatcher: Dispatching action ${action.getClass.getSimpleName}")
      _callbacks.foreach(_.apply(action))
      println(s"  Dispatcher: Dispatching action Action.Done(${action.getClass.getSimpleName})")
      _callbacks.foreach(_.apply(action))
      isDispatching = false

      _dispatchedActions += action

      Future.successful((): Unit)
    }

    // ******************* Additional API for testing ******************* //
    def dispatchedActions: Seq[Action] = _dispatchedActions.toVector
    def callbacks: Seq[Action => Future[Unit]] = _callbacks.toVector
  }
}

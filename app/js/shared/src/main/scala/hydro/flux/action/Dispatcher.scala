package hydro.flux.action

import hydro.common.JsLoggingUtils
import hydro.common.JsLoggingUtils.logExceptions

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Failure
import scala.util.Success

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
  private def nullCallback: PartialFunction[Action, Future[Unit]] = { case _ =>
    Future.successful((): Unit)
  }

  private[flux] final class Impl extends Dispatcher {
    private[flux] var callbacks: Set[Action => Future[Unit]] = Set()
    private var isDispatching: Boolean = false

    def registerAsync(callback: Action => Future[Unit]) = {
      require(!isDispatching)
      callbacks = callbacks + callback
    }

    def dispatch(action: Action) = {
      require(!isDispatching, s"Dispatch triggered action $action")

      invokeCallbacks(action)
        .transformWith {
          case Success(_) => invokeCallbacks(StandardActions.Done(action))
          case Failure(e) => invokeCallbacks(StandardActions.Failed(action)).map(_ => logExceptions(throw e))
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

  final class Fake extends Dispatcher {
    private val delegate: Dispatcher.Impl = new Impl
    private val _dispatchedActions: mutable.Buffer[Action] = mutable.Buffer()

    // ******************* Implementation of Dispatcher interface ******************* //
    override def registerAsync(callback: Action => Future[Unit]): Unit = delegate.registerAsync(callback)

    override def dispatch(action: Action): Future[Unit] = async {
      await(delegate.dispatch(action))
      _dispatchedActions += action
    }

    // ******************* Additional API for testing ******************* //
    def dispatchedActions: Seq[Action] = _dispatchedActions.toVector
    def callbacks: Seq[Action => Future[Unit]] = delegate.callbacks.toVector
  }
}

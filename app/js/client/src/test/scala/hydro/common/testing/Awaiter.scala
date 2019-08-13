package hydro.common.testing

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object Awaiter {

  def expectEventually: EventuallyAwaiter = new EventuallyAwaiter
  def expectConsistently: ConsistentlyAwaiter = new ConsistentlyAwaiter

  sealed abstract class AwaiterWithType {
    protected def verb: String
    protected def expectCondition(condition: => Boolean, onFail: => Unit): Future[Unit]

    def equal[T](a: => T, b: => T): Future[Unit] = {
      expectCondition(a == b, throw new AssertionError(s"Expected $a == $b to be $verb true"))
    }

    def nonEmpty[T](iterable: => Iterable[T]): Future[Unit] = {
      expectCondition(
        iterable.nonEmpty,
        throw new AssertionError(s"Expected given iterable to be $verb non-empty"))
    }

    def isEmpty[T](iterable: => Iterable[T]): Future[Unit] = {
      expectCondition(
        iterable.isEmpty,
        throw new AssertionError(s"Expected given iterable to be $verb empty"))
    }
  }
  final class EventuallyAwaiter extends AwaiterWithType {
    override protected def verb = "eventually"
    override protected def expectCondition(condition: => Boolean, onFail: => Unit) = {
      val resultPromise = Promise[Unit]()

      def cyclicLogic(cycleCount: Int = 0): Unit = {
        if (condition) {
          resultPromise.success((): Unit)
        } else {
          if (cycleCount > 100) {
            resultPromise.completeWith(
              Future(onFail).flatMap(_ => Future.failed(new AssertionError(s"expect $verb timed out"))))
          } else {
            js.timers.setTimeout(5.milliseconds)(cyclicLogic(cycleCount = cycleCount + 1))
          }
        }
      }
      cyclicLogic()

      resultPromise.future
    }

    def complete[T](future: Future[T], expected: T = null): Future[Unit] = {
      val resultPromise = Promise[Unit]()
      future.map(value =>
        if (value == ((): Unit) || value == expected) {
          resultPromise.trySuccess((): Unit)
        } else {
          resultPromise.tryFailure(
            new java.lang.AssertionError(
              s"Expected future to be completed with value $expected, but got $value"))
      })
      js.timers.setTimeout(500.milliseconds) {
        resultPromise.tryFailure(
          new java.lang.AssertionError(s"future completion timed out (expected $expected)"))
      }
      resultPromise.future
    }
  }
  final class ConsistentlyAwaiter extends AwaiterWithType {
    override protected def verb = "consistently"
    override protected def expectCondition(condition: => Boolean, onFail: => Unit) = {
      val resultPromise = Promise[Unit]()

      def cyclicLogic(cycleCount: Int = 0): Unit = {
        if (!condition) {
          resultPromise.completeWith(
            Future(onFail).flatMap(_ => Future.failed(new AssertionError(s"expect $verb failed"))))
        } else {
          if (cycleCount > 20) {
            resultPromise.success((): Unit)
          } else {
            js.timers.setTimeout(5.milliseconds)(cyclicLogic(cycleCount = cycleCount + 1))
          }
        }
      }
      cyclicLogic()

      resultPromise.future
    }

    def neverComplete(future: Future[_]): Future[Unit] = {
      val resultPromise = Promise[Unit]()
      future.onComplete(
        tryValue =>
          resultPromise.tryFailure(new java.lang.AssertionError(
            s"Expected future that never completes, but completed with tryValue $tryValue")))
      js.timers.setTimeout(100.milliseconds) {
        resultPromise.trySuccess((): Unit)
      }
      resultPromise.future
    }
  }
}

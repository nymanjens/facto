package common.testing

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object Awaiter {

  def expectEventuallyEqual[T](a: => T, b: => T): Future[Unit] = {
    expectEventually(a == b, throw new AssertionError(s"Expected $a == $b to be eventually true"))
  }

  def expectEventually(condition: => Boolean, onTimeout: => Unit): Future[Unit] = {
    val resultPromise = Promise[Unit]()

    def cyclicLogic(cycleCount: Int = 0): Unit = {
      if (condition) {
        resultPromise.success((): Unit)
      } else {
        if (cycleCount > 100) {
          resultPromise.completeWith(
            Future(onTimeout).flatMap(_ => Future.failed(new AssertionError("expectEventually timed out"))))
        }
        js.timers.setTimeout(5.milliseconds)(cyclicLogic(cycleCount = cycleCount + 1))
      }
    }
    cyclicLogic()

    resultPromise.future
  }

  def expectComplete[T](future: Future[T], expected: T = null): Future[Unit] = {
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

  def expectNeverComplete(future: Future[_]): Future[Unit] = {
    val resultPromise = Promise[Unit]()
    future.map(
      value =>
        resultPromise.tryFailure(new java.lang.AssertionError(
          s"Expected future that never completes, but completed with value $value")))
    js.timers.setTimeout(500.milliseconds) {
      resultPromise.trySuccess((): Unit)
    }
    resultPromise.future
  }
}

package models.access

import utest._

import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}

object FutureLocalDatabaseTest extends TestSuite {

  override def tests = TestSuite {
    val unsafeLocalDatabasePromise = Promise[LocalDatabase]()
    val futureLocalDatabase =
      new FutureLocalDatabase(unsafeLocalDatabaseFuture = unsafeLocalDatabasePromise.future)
    val localDatabase: LocalDatabase = null // Too cumbersome to depend on the chaning interface

    "future(safe = true)" - async {
      unsafeLocalDatabasePromise.failure(new IllegalArgumentException("Test error"))
      val future = futureLocalDatabase.future(safe = true)

      await(expectNeverComplete(future))
    }
    "future(safe = false)" - async {
      val exception = new IllegalArgumentException("Test error")
      unsafeLocalDatabasePromise.failure(exception)
      val future = futureLocalDatabase.future(safe = false)

      await(future.transform(value => {
        value ==> Failure(exception)
        Success(localDatabase)
      }))
    }
    "future(includesLatestUpdates = false)" - async {
      futureLocalDatabase.scheduleUpdateAtStart(_ => Promise().future)
      val future = futureLocalDatabase.future(safe = false, includesLatestUpdates = false)

      await(expectNeverComplete(future))

      unsafeLocalDatabasePromise.success(localDatabase)
      await(expectComplete(future, expected = localDatabase))
    }

    "scheduleUpdateAt{Start,End}()" - async {
      val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)
      unsafeLocalDatabasePromise.success(localDatabase)

      await(expectComplete(updateAtStart.wasCalledFuture))
      updateAtEnd.wasCalled ==> false
      updateAtEnd2.wasCalled ==> false

      updateAtStart.set()

      updateAtStart.wasCalled ==> true
      await(expectComplete(updateAtEnd.wasCalledFuture))
      updateAtEnd2.wasCalled ==> false

      updateAtEnd.set()

      await(expectComplete(updateAtEnd2.wasCalledFuture))

      updateAtEnd2.set()
      val updateAtEnd3 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)

      await(expectComplete(updateAtEnd3.wasCalledFuture))
    }

    "future(includesLatestUpdates = true)" - async {
      val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)
      var future = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)

      await(expectNeverComplete(future))

      unsafeLocalDatabasePromise.success(localDatabase)

      await(expectNeverComplete(future))

      updateAtStart.set()

      await(expectNeverComplete(future))

      updateAtEnd.set()

      await(expectComplete(future, localDatabase))

      val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      future = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)

      await(expectNeverComplete(future))

      updateAtEnd2.set()
      await(expectComplete(future, localDatabase))
    }
  }

  private def expectNeverComplete(future: Future[_]): Future[Unit] = {
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
  private def expectComplete[T](future: Future[T], expected: T = null): Future[Unit] = {
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

  class FakeUpdateFunction {
    private var wasCalledPromise: Promise[Unit] = Promise()
    private val resultPromise: Promise[Unit] = Promise()

    private def function(localDatabase: LocalDatabase): Future[Unit] = {
      require(!wasCalledPromise.isCompleted)
      wasCalledPromise.success((): Unit)
      resultPromise.future
    }

    def wasCalled: Boolean = wasCalledPromise.isCompleted
    def wasCalledFuture: Future[Unit] = wasCalledPromise.future
    def set(): Unit = resultPromise.success((): Unit)
  }
  object FakeUpdateFunction {
    def createAndAdd(adder: (LocalDatabase => Future[Unit]) => Unit): FakeUpdateFunction = {
      val result = new FakeUpdateFunction()
      adder(result.function)
      result
    }
  }
}

package models.access

import common.testing.Awaiter
import utest._

import scala.async.Async.{async, await}
import scala.concurrent.{Future, Promise}
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
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

      await(Awaiter.expectNeverComplete(future))
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

      await(Awaiter.expectNeverComplete(future))

      unsafeLocalDatabasePromise.success(localDatabase)
      await(Awaiter.expectComplete(future, expected = localDatabase))
    }

    "scheduleUpdateAt{Start,End}()" - async {
      val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)
      unsafeLocalDatabasePromise.success(localDatabase)

      await(Awaiter.expectComplete(updateAtStart.wasCalledFuture))
      updateAtEnd.wasCalled ==> false
      updateAtEnd2.wasCalled ==> false

      updateAtStart.set()

      updateAtStart.wasCalled ==> true
      await(Awaiter.expectComplete(updateAtEnd.wasCalledFuture))
      updateAtEnd2.wasCalled ==> false

      updateAtEnd.set()

      await(Awaiter.expectComplete(updateAtEnd2.wasCalledFuture))

      updateAtEnd2.set()
      val updateAtEnd3 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)

      await(Awaiter.expectComplete(updateAtEnd3.wasCalledFuture))
    }

    "future(includesLatestUpdates = true)" - async {
      val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)
      var future = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)

      await(Awaiter.expectNeverComplete(future))

      unsafeLocalDatabasePromise.success(localDatabase)

      await(Awaiter.expectNeverComplete(future))

      updateAtStart.set()

      await(Awaiter.expectNeverComplete(future))

      updateAtEnd.set()

      await(Awaiter.expectComplete(future, localDatabase))

      val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      future = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)

      await(Awaiter.expectNeverComplete(future))

      updateAtEnd2.set()
      await(Awaiter.expectComplete(future, localDatabase))
    }
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

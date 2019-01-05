package hydro.models.access

import hydro.common.testing.Awaiter
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Failure
import scala.util.Success

object FutureLocalDatabaseTest extends TestSuite {

  override def tests = TestSuite {
    val unsafeLocalDatabasePromise = Promise[LocalDatabase]()
    val futureLocalDatabase =
      new FutureLocalDatabase(unsafeLocalDatabaseFuture = unsafeLocalDatabasePromise.future)
    val localDatabase: LocalDatabase = null // Too cumbersome to depend on the changing interface

    "databasePromise throws exception" - {
      val exception = new IllegalArgumentException("Test error")
      unsafeLocalDatabasePromise.failure(exception)

      "future(safe = true)" - async {
        val future = futureLocalDatabase.future(safe = true)

        await(Awaiter.expectConsistently.neverComplete(future))
      }
      "future(safe = false)" - async {
        val future = futureLocalDatabase.future(safe = false)

        await(future.transform {
          case Success(_) => throw new java.lang.AssertionError("Expected failure")
          case Failure(thisException) =>
            thisException ==> exception
            Success(null)
        })
      }
      "option()" - async {
        def option = futureLocalDatabase.option()

        await(Awaiter.expectConsistently.equal(option, None))
      }
    }
    "includesLatestUpdates = false" - {
      futureLocalDatabase.scheduleUpdateAtStart(_ => Promise().future)
      val future = futureLocalDatabase.future(safe = false, includesLatestUpdates = false)
      def option = futureLocalDatabase.option(includesLatestUpdates = false)

      "database not yet resolved" - {
        "future" - Awaiter.expectConsistently.neverComplete(future)
        "option" - Awaiter.expectConsistently.equal(option, None)
      }
      "database resolved" - {
        unsafeLocalDatabasePromise.success(localDatabase)

        "future" - Awaiter.expectEventually.complete(future, expected = localDatabase)
        "option" - Awaiter.expectEventually.equal(option, Some(localDatabase))
      }
    }

    "includesLatestUpdates = true" - {
      val future = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)
      def option = futureLocalDatabase.option(includesLatestUpdates = true)

      "without updates" - {
        "database not yet resolved" - {
          "future" - Awaiter.expectConsistently.neverComplete(future)
          "option" - Awaiter.expectConsistently.equal(option, None)
        }
        "database resolved" - {
          unsafeLocalDatabasePromise.success(localDatabase)

          "future" - Awaiter.expectEventually.complete(future, expected = localDatabase)
          "option" - Awaiter.expectEventually.equal(option, Some(localDatabase))
        }
      }
      "with updates" - {
        val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
        val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)

        "database not yet resolved" - {
          "future" - Awaiter.expectConsistently.neverComplete(future)
          "option" - Awaiter.expectConsistently.equal(option, None)
        }
        "database resolved" - {
          unsafeLocalDatabasePromise.success(localDatabase)

          "updateAtStart resolved" - {
            updateAtStart.set()
            "future" - Awaiter.expectConsistently.neverComplete(future)
            "option" - Awaiter.expectConsistently.equal(option, None)

            "updateAtEnd resolved" - {
              updateAtEnd.set()
              "future" - Awaiter.expectEventually.complete(future, localDatabase)
              "option" - Awaiter.expectEventually.equal(option, Some(localDatabase))

              "with added updateAtEnd2" - async {
                await(Awaiter.expectEventually.complete(future, localDatabase)) // Wait for future to complete

                val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
                val future2 = futureLocalDatabase.future(safe = false, includesLatestUpdates = true)

                await(Awaiter.expectConsistently.neverComplete(future2))
                await(Awaiter.expectConsistently.equal(option, None))

                updateAtEnd2.set()

                await(Awaiter.expectEventually.complete(future2, localDatabase))
                await(Awaiter.expectEventually.equal(option, Some(localDatabase)))
              }
            }
          }
        }
      }
    }

    "scheduleUpdateAt{Start,End}()" - async {
      val updateAtEnd = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtEnd2 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)
      val updateAtStart = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtStart)
      unsafeLocalDatabasePromise.success(localDatabase)

      await(Awaiter.expectEventually.complete(updateAtStart.wasCalledFuture))
      updateAtEnd.wasCalled ==> false
      updateAtEnd2.wasCalled ==> false

      updateAtStart.set()

      updateAtStart.wasCalled ==> true
      await(Awaiter.expectEventually.complete(updateAtEnd.wasCalledFuture))
      updateAtEnd2.wasCalled ==> false

      updateAtEnd.set()

      await(Awaiter.expectEventually.complete(updateAtEnd2.wasCalledFuture))

      updateAtEnd2.set()
      val updateAtEnd3 = FakeUpdateFunction.createAndAdd(futureLocalDatabase.scheduleUpdateAtEnd)

      await(Awaiter.expectEventually.complete(updateAtEnd3.wasCalledFuture))
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

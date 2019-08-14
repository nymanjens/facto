package hydro.common

import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.async
import scala.async.Async.await
import java.util.concurrent.atomic.AtomicLong

import hydro.common.testing.Awaiter
import utest._
import utest.TestSuite

import scala.concurrent.Future
import scala.concurrent.Promise

object SerializingTaskQueueJsTest extends TestSuite {

  override def tests = TestSuite {

    "create()" - {
      val queue = SerializingTaskQueue.create()
      val task1 = Promise[String]()
      val task2 = Promise[String]()
      val task1Counter = new AtomicLong
      val task2Counter = new AtomicLong

      val task1Result = queue.schedule {
        task1Counter.incrementAndGet()
        task1.future
      }
      val task2Result = queue.schedule {
        task2Counter.incrementAndGet()
        task2.future
      }

      "0 tasks complete" - async {
        await(
          combined(
            Awaiter.expectConsistently.neverComplete(task1Result),
            Awaiter.expectConsistently.neverComplete(task2Result),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectConsistently.equal(task2Counter.get(), 0L),
          ))
      }

      "1 task complete" - async {
        task1.success("a")

        await(
          combined(
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectConsistently.neverComplete(task2Result),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
          ))
      }

      "2 tasks complete" - async {
        task1.success("a")
        task2.success("b")

        await(
          combined(
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectEventually.complete(task2Result, expected = "b"),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
          )
        )
      }
    }

    "withAtMostSingleQueuedTask()" - {
      val queue = SerializingTaskQueue.withAtMostSingleQueuedTask()
      val task1 = Promise[String]()
      val task2 = Promise[String]()
      val task1Counter = new AtomicLong
      val task2Counter = new AtomicLong

      val task1Result = queue.schedule {
        task1Counter.incrementAndGet()
        task1.future
      }

      "0 tasks complete" - async {
        await(Awaiter.expectEventually.equal(task1Counter.get(), 1L))

        val task2Result = queue.schedule {
          task2Counter.incrementAndGet()
          task2.future
        }

        await(
          combined(
            Awaiter.expectConsistently.neverComplete(task1Result),
            Awaiter.expectConsistently.neverComplete(task2Result),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectConsistently.equal(task2Counter.get(), 0L),
          ))

        expectIsFailure(queue.schedule(Future.successful((): Unit)))
      }

      "1 task complete" - async {
        await(Awaiter.expectEventually.equal(task1Counter.get(), 1L))

        val task2Result = queue.schedule {
          task2Counter.incrementAndGet()
          task2.future
        }

        task1.success("a")

        await(
          combined(
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectConsistently.neverComplete(task2Result),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
          ))

        val task3Result = queue.schedule(Future.successful((): Unit))
        await(Awaiter.expectConsistently.neverComplete(task3Result))

        expectIsFailure(queue.schedule(Future.successful((): Unit)))
      }

      "2 tasks complete" - async {
        await(Awaiter.expectEventually.equal(task1Counter.get(), 1L))

        val task2Result = queue.schedule {
          task2Counter.incrementAndGet()
          task2.future
        }

        task1.success("a")
        task2.success("b")

        await(
          combined(
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectEventually.complete(task2Result, expected = "b"),
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
          )
        )
      }
    }
  }

  private def combined(futures: Future[Unit]*): Future[Unit] = Future.sequence(futures).map(_ => (): Unit)

  private def expectIsFailure(future: Future[Unit]): Unit = {
    future.isCompleted ==> true
    future.value.get.isFailure ==> true
  }
}

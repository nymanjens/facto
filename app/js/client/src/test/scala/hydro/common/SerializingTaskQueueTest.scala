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

object SerializingTaskQueueTest extends TestSuite {

  override def tests = TestSuite {

    "schedule()" - {
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
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectConsistently.equal(task2Counter.get(), 0L),
            Awaiter.expectConsistently.neverComplete(task1Result),
            Awaiter.expectConsistently.neverComplete(task2Result),
          ))
      }

      "1 task complete" - async {
        task1.success("a")

        await(
          combined(
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectConsistently.neverComplete(task2Result),
          ))
      }

      "2 tasks complete" - async {
        task1.success("a")
        task2.success("b")

        await(
          combined(
            Awaiter.expectEventually.equal(task1Counter.get(), 1L),
            Awaiter.expectEventually.equal(task2Counter.get(), 1L),
            Awaiter.expectEventually.complete(task1Result, expected = "a"),
            Awaiter.expectEventually.complete(task2Result, expected = "b"),
          )
        )
      }
    }
  }

  private def combined(futures: Future[Unit]*): Future[Unit] = Future.sequence(futures).map(_ => (): Unit)
}

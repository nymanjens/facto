package hydro.common

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicLong

import hydro.common.testing.Awaiter
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scala.concurrent.Promise

object SerializingTaskQueueJvmTest extends Specification {
  isolated

  "create()" should {
    val queue = SerializingTaskQueue.create()
    val task1 = Promise[String]()
    val task2 = Promise[String]()
    val task1Counter = new AtomicLong
    val task2Counter = new AtomicLong

    val task1Result = queue.schedule {
      Future((): Unit).flatMap { _ =>
        task1Counter.incrementAndGet()
        task1.future
      }
    }
    val task2Result = queue.schedule {
      Future((): Unit).flatMap { _ =>
        task2Counter.incrementAndGet()
        task2.future
      }
    }

    "0 tasks complete" in {
      Awaiter.expectConsistently.neverComplete(task1Result)
      Awaiter.expectConsistently.neverComplete(task2Result)
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectConsistently.equal(task2Counter.get(), 0L)
    }

    "1 task complete" in {
      task1.success("a")

      Awaiter.expectEventually.complete(task1Result, expected = "a")
      Awaiter.expectConsistently.neverComplete(task2Result)
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectEventually.equal(task2Counter.get(), 1L)
    }

    "2 tasks complete" in {
      task1.success("a")
      task2.success("b")

      Awaiter.expectEventually.complete(task1Result, expected = "a")
      Awaiter.expectEventually.complete(task2Result, expected = "b")
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectEventually.equal(task2Counter.get(), 1L)
    }
  }

  "withAtMostSingleQueuedTask()" should {
    val queue = SerializingTaskQueue.withAtMostSingleQueuedTask()
    val task1 = Promise[String]()
    val task2 = Promise[String]()
    val task1Counter = new AtomicLong
    val task2Counter = new AtomicLong

    val task1Result = queue.schedule {
      Future((): Unit).flatMap { _ =>
        task1Counter.incrementAndGet()
        task1.future
      }
    }

    "0 tasks complete" in {
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)

      val task2Result = queue.schedule {
        Future((): Unit).flatMap { _ =>
          task2Counter.incrementAndGet()
          task2.future
        }
      }

      Awaiter.expectConsistently.neverComplete(task1Result)
      Awaiter.expectConsistently.neverComplete(task2Result)
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectConsistently.equal(task2Counter.get(), 0L)

      expectIsFailure(queue.schedule(Future.successful((): Unit)))
    }

    "1 task complete" in {
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)

      val task2Result = queue.schedule {
        Future((): Unit).flatMap { _ =>
          task2Counter.incrementAndGet()
          task2.future
        }
      }

      task1.success("a")

      Awaiter.expectEventually.complete(task1Result, expected = "a")
      Awaiter.expectConsistently.neverComplete(task2Result)
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectEventually.equal(task2Counter.get(), 1L)

      val task3Result = queue.schedule(Future.successful((): Unit))
      Awaiter.expectConsistently.neverComplete(task3Result)

      expectIsFailure(queue.schedule(Future.successful((): Unit)))
    }

    "2 tasks complete" in {
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)

      val task2Result = queue.schedule {
        Future((): Unit).flatMap { _ =>
          task2Counter.incrementAndGet()
          task2.future
        }
      }

      task1.success("a")
      task2.success("b")

      Awaiter.expectEventually.complete(task1Result, expected = "a")
      Awaiter.expectEventually.complete(task2Result, expected = "b")
      Awaiter.expectEventually.equal(task1Counter.get(), 1L)
      Awaiter.expectEventually.equal(task2Counter.get(), 1L)
    }
  }

  private def expectIsFailure(future: Future[Unit]): MatchResult[Any] = {
    future.isCompleted mustEqual true
    future.value.get.isFailure mustEqual true
  }
}

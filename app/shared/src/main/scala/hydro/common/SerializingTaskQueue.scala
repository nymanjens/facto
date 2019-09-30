package hydro.common

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import hydro.common
import hydro.common.Annotations.guardedBy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SerializingTaskQueue {
  def schedule[T](task: => Future[T]): Future[T]
}
object SerializingTaskQueue {
  def create(): SerializingTaskQueue = new WithInfiniteQueue()
  def withAtMostSingleQueuedTask(): SerializingTaskQueue = new WithAtMostSingleQueuedTask()

  private class WithInfiniteQueue extends SerializingTaskQueue {
    @guardedBy("this")
    private var mostRecentlyAddedTaskFuture: Future[_] = Future.successful(null)

    override def schedule[T](task: => Future[T]): Future[T] = {
      this.synchronized {
        val result: Future[T] = mostRecentlyAddedTaskFuture.flatMap(_ => task)
        mostRecentlyAddedTaskFuture = result.recover {
          case throwable: Throwable =>
            println(s"  Caught exception in SerializingTaskQueue: $throwable")
            throwable.printStackTrace()
            null
        }
        result
      }
    }
  }

  private class WithAtMostSingleQueuedTask extends SerializingTaskQueue {
    private val delegate: SerializingTaskQueue = new WithInfiniteQueue()
    @guardedBy("this")
    private var queueContainsTask: Boolean = false

    override def schedule[T](task: => Future[T]): Future[T] = {
      this.synchronized {
        if (queueContainsTask) {
          Future.failed(new RuntimeException("Task was not scheduled"))
        } else {
          queueContainsTask = true
          delegate.schedule {
            this.synchronized {
              queueContainsTask = false
            }
            task
          }
        }
      }
    }
  }
}

package common

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext

class SinglePendingTaskQueue {
  private implicit val delegate: ExecutionContext = JSExecutionContext.queue
  private var hasPendingTask: Boolean = false

  def execute[T](body: => T): Option[Future[T]] = {
    if (hasPendingTask) {
      None
    } else {
      hasPendingTask = true
      Some(
        Future {
          hasPendingTask = false
          body
        }
      )
    }
  }
}

object SinglePendingTaskQueue {
  def create(): SinglePendingTaskQueue = new SinglePendingTaskQueue()
}

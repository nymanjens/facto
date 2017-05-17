package common

import common.LoggingUtils.logExceptions

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js.timers._

class SinglePendingTaskQueue {
  private implicit val delegate: ExecutionContext = JSExecutionContext.queue
  private var hasPendingTask: Boolean = false

  def execute[T](body: => T): Unit = {
    if (hasPendingTask) {
      None
    } else {
      hasPendingTask = true
      Some(
        setTimeout(1) {
          logExceptions {
            hasPendingTask = false
            body
          }
        }
      )
    }
  }
}

object SinglePendingTaskQueue {
  def create(): SinglePendingTaskQueue = new SinglePendingTaskQueue()
}

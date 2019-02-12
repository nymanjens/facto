package hydro.common

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class SerializingTaskQueue {
  private var mostRecentlyAddedTaskFuture: Future[_] = Future.successful(null)

  def schedule[T](task: => Future[T]): Future[T] = {
    val result: Future[T] = mostRecentlyAddedTaskFuture.flatMap(_ => task)
    mostRecentlyAddedTaskFuture = result
    result
  }
}

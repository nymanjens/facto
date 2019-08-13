package hydro.common

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SerializingTaskQueue {
  private var mostRecentlyAddedTaskFuture: Future[_] = Future.successful(null)

  def schedule[T](task: => Future[T]): Future[T] = {
    val result: Future[T] = mostRecentlyAddedTaskFuture.flatMap(_ => task)
    mostRecentlyAddedTaskFuture = result
    result
  }
}

package common

import japgolly.scalajs.react.CallbackTo
import org.scalajs.dom.console

import scala.concurrent.{ExecutionContext, Future}

object LoggingUtils {

  def logExceptions[T](codeBlock: => T): T = {
    try {
      codeBlock
    } catch {
      case t: Throwable =>
        console.log(s"  Caught exception: $t")
        t.printStackTrace()
        throw t
    }
  }
  def logFailure[T](future: Future[T])(implicit executor: ExecutionContext): Future[T] = {
    future.failed.foreach { t =>
      console.log(s"  Caught exception: $t")
      t.printStackTrace()
    }
    future
  }

  def LogExceptionsCallback[T](codeBlock: => T): CallbackTo[T] = {
    CallbackTo(logExceptions(codeBlock))
  }

  def LogExceptionsFuture[T](codeBlock: => T)(implicit ec: ExecutionContext): Future[T] = {
    Future(logExceptions(codeBlock))
  }
}

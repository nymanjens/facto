package hydro.common

import japgolly.scalajs.react.CallbackTo
import org.scalajs.dom.console

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object JsLoggingUtils {

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
  def logFailure[T](future: => Future[T])(implicit executor: ExecutionContext): Future[T] = {
    val theFuture = logExceptions(future)
    theFuture.failed.foreach { t =>
      console.log(s"  Caught exception: $t")
      t.printStackTrace()
    }
    theFuture
  }

  def LogExceptionsCallback[T](codeBlock: => T): CallbackTo[T] = {
    CallbackTo(logExceptions(codeBlock))
  }

  def LogExceptionsFuture[T](codeBlock: => T)(implicit ec: ExecutionContext): Future[T] = {
    Future(logExceptions(codeBlock))
  }
}

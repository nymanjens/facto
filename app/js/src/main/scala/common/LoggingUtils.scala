package common

import japgolly.scalajs.react.{Callback, CallbackTo}

object LoggingUtils {

  def logExceptions[T](codeBlock: => T): T = {
    try {
      codeBlock
    } catch {
      case t: Throwable =>
        println(s"  Caught exception: $t")
        t.printStackTrace()
        throw t
    }
  }

  def LogExceptionsCallback[T](codeBlock: => T): CallbackTo[T] = {
    CallbackTo(logExceptions(codeBlock))
  }
}

package common

import scala.annotation.StaticAnnotation
import scala.concurrent._

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
}

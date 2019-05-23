package hydro.common

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

package hydro.common

import scala.annotation.StaticAnnotation
import scala.concurrent._

object ScalaUtils {

  /** Returns the name of the object as defined by "object X {}" */
  def objectName(obj: AnyRef): String = {
    obj.getClass.getSimpleName.replace("$", "")
  }

  def callbackSettingFuturePair(): (() => Unit, Future[Unit]) = {
    val promise = Promise[Unit]()
    val callback: () => Unit = () => promise.success()
    (callback, promise.future)
  }

  def toPromise[T](future: Future[T]): Promise[T] = Promise[T]().completeWith(future)

  def ifThenOption[T](condition: Boolean)(value: => T): Option[T] = {
    if (condition) {
      Some(value)
    } else {
      None
    }
  }

  def stripRequiredPrefix(s: String, prefix: String): String = {
    require(s.startsWith(prefix), s"string doesn't start with prefix: prefix = $prefix, string = $s")
    s.stripPrefix(prefix)
  }

  /** Scala version of com.google.common.annotations.VisibleForTesting. */
  class visibleForTesting extends StaticAnnotation

  /** Scala version of javax.annotations.Nullable. */
  class nullable extends StaticAnnotation

  /** Scala version of GuardedBy. */
  class guardedBy(objectName: String) extends StaticAnnotation
}

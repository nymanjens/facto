package common

import scala.annotation.{StaticAnnotation, tailrec}
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

  /** Scala version of com.google.common.annotations.VisibleForTesting. */
  class visibleForTesting extends StaticAnnotation
  /** Scala version of javax.annotations.Nullable. */
  class nullable extends StaticAnnotation
}

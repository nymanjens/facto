package common

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
}

package common

object ScalaUtils {

  /** Returns the name of the object as defined by "object X {}" */
  def objectName(obj: AnyRef): String = {
    obj.getClass.getSimpleName.replace("$", "")
  }
}

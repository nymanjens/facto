package scala2js

import scala.scalajs.js

object Scala2Js {

  trait Converter[T] {
    def toJs(value: T): js.Any
    def toScala(value: js.Any): T
  }

  trait MapConverter[T] extends Converter[T] {
    override def toJs(value: T): js.Dictionary[js.Any]
    final override def toScala(value: js.Any): T = toScala(value.asInstanceOf[js.Dictionary[js.Any]])
    def toScala(value: js.Dictionary[js.Any]): T

    // **************** Protected helper methods **************** //
    protected final def getRequiredValueFromDict[T: Converter](value: js.Dictionary[js.Any])(key: String): T = {
      require(value.contains(key), s"Key $key is missing from ${js.JSON.stringify(value)}")
      Scala2Js.toScala[T](value(key))
    }
  }

  def toJs[T: Converter](value: T): js.Any = {
    implicitly[Converter[T]].toJs(value)
  }

  def toJsMap[T: MapConverter](value: T): js.Dictionary[js.Any] = {
    implicitly[MapConverter[T]].toJs(value)
  }

  def toScala[T: Converter](value: js.Any): T = {
    implicitly[Converter[T]].toScala(value)
  }
}

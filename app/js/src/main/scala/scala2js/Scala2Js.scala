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
    protected final def getRequiredValueFromDict[V: Converter](value: js.Dictionary[js.Any])(key: String): V = {
      require(value.contains(key), s"Key $key is missing from ${js.JSON.stringify(value)}")
      Scala2Js.toScala[V](value(key))
    }

    protected final def getOptionalValueFromDict[V: Converter](value: js.Dictionary[js.Any])(key: String): Option[V] = {
      value.get(key) map Scala2Js.toScala[V]
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

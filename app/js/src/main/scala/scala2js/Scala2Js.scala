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
    protected final def getRequiredValueFromDict[V: Converter](dict: js.Dictionary[js.Any])(
        key: Key[V, _]): V = {
      require(dict.contains(key.name), s"Key ${key.name} is missing from ${js.JSON.stringify(dict)}")
      Scala2Js.toScala[V](dict(key.name))
    }

    protected final def getOptionalValueFromDict[V: Converter](value: js.Dictionary[js.Any])(
        key: String): Option[V] = {
      value.get(key) map Scala2Js.toScala[V]
    }
  }

  /**
    * @param name The name of this key in a js dictionary
    * @tparam V The scala type of the values
    * @tparam E The scala type corresponding to the js dictionary of which this key is a part
    */
  case class Key[V: Converter, E](name: String)
  object Key {
    def toJsPair[V: Converter, E](keyValuePair: (Key[V, E], V)): (String, js.Any) =
      keyValuePair match {
        case (key, value) => key.name -> toJs(value)
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

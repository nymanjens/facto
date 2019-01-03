package hydro.scala2js

import hydro.models.access.ModelField

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object Scala2Js {

  trait Converter[T] {
    def toJs(value: T): js.Any
    def toScala(value: js.Any): T
  }

  trait MapConverter[T] extends Converter[T] {
    override def toJs(value: T): js.Dictionary[js.Any]
    final override def toScala(value: js.Any): T = toScala(value.asInstanceOf[js.Dictionary[js.Any]])
    def toScala(value: js.Dictionary[js.Any]): T
  }

  def toJs[T: Converter](value: T): js.Any = {
    implicitly[Converter[T]].toJs(value)
  }

  def toJs[T: Converter](values: Iterable[T]): js.Array[js.Any] = {
    values.map(implicitly[Converter[T]].toJs).toJSArray
  }

  def toJsMap[T: MapConverter](value: T): js.Dictionary[js.Any] = {
    implicitly[MapConverter[T]].toJs(value)
  }

  def toScala[T: Converter](value: js.Any): T = {
    implicitly[Converter[T]].toScala(value)
  }

  def toJs[V, E](value: V, field: ModelField[V, E]): js.Any =
    toJs(value)(StandardConverters.fromModelField(field))
}

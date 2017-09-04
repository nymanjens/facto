package jsfacades

import japgolly.scalajs.react.{Children, JsComponent}

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala2js.Converters._

object ReactTagInput {

  // **************** API ****************//
  def apply(tags: Seq[String] = Seq(),
            suggestions: Seq[String],
            handleAddition: String => Unit,
            handleDelete: String => Unit,
            handleDrag: DragHandler) = {
    val component = JsComponent[js.Object, Children.None, Null](js.Dynamic.global.ReactTags.WithContext)
    component(
      Props(
        tags = tags.map(toTagObject _).toJSArray,
        suggestions = suggestions.toJSArray,
        handleAddition = handleAddition,
        handleDelete = handleDelete,
        handleDrag = handleDrag.onDragged(_, _, _)
      ).toJsObject)
  }

  // **************** Public inner types ****************//
  trait DragHandler {
    def onDragged(tag: String, currentPos: Int, newPos: Int): Unit
  }

  private def toTagObject(tag: String): js.Object = js.Dynamic.literal(id = tag, text = tag)

  // **************** Private inner types ****************//
  private case class Props(tags: js.Array[js.Object],
                           suggestions: js.Array[String],
                           handleAddition: js.Function1[String, Unit],
                           handleDelete: js.Function1[String, Unit],
                           handleDrag: js.Function3[String, Int, Int, Unit]) {
    def toJsObject: js.Object =
      js.Dynamic.literal(
        tags = tags,
        suggestions = suggestions,
        handleAddition = handleAddition,
        handleDelete = handleDelete,
        handleDrag = handleDrag)
  }
}

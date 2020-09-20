package hydro.jsfacades

import japgolly.scalajs.react.Children
import japgolly.scalajs.react.JsComponent
import org.scalajs.dom.ext.KeyCode

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSImport

object ReactTagInput {

  // **************** API ****************//
  def apply(
      tags: Seq[String] = Seq(),
      suggestions: Seq[String],
      handleAddition: String => Unit,
      handleDelete: DeleteHandler,
      handleDrag: DragHandler,
      delimiters: Seq[Int] = Seq(KeyCode.Enter, KeyCode.Tab),
      minQueryLength: Int = 2,
      classNames: Map[String, String] = Map(),
      autoFocus: Boolean = false,
  ) = {
    val component = JsComponent[js.Object, Children.None, Null](RawComponent)
    component(
      Props(
        tags = tags.zipWithIndex.map { case (tag, i) => TagObject(i, tag) }.toJSArray,
        suggestions = suggestions.toJSArray,
        handleAddition = handleAddition,
        handleDelete = pos => handleDelete.onDeleted(pos, tags(pos)),
        handleDrag =
          (tagObject, currentPos, newPos) => handleDrag.onDragged(tagObject.text, currentPos, newPos),
        delimiters = delimiters.toJSArray,
        minQueryLength = minQueryLength,
        classNames = classNames.toJSDictionary,
        autoFocus,
      ).toJsObject
    )
  }

  // **************** Public inner types ****************//
  trait DeleteHandler {
    def onDeleted(pos: Int, tag: String): Unit
  }
  trait DragHandler {
    def onDragged(tag: String, currentPos: Int, newPos: Int): Unit
  }

  // **************** Private inner types ****************//
  @JSImport("react-tag-input", "WithContext")
  @js.native
  private object RawComponent extends js.Object

  @js.native
  private trait TagObject extends js.Object {
    def id: Int = js.native
    def text: String = js.native
  }
  private object TagObject {
    def apply(id: Int, tag: String): js.Object = js.Dynamic.literal(id = id, text = tag)
  }

  private case class Props(
      tags: js.Array[js.Object],
      suggestions: js.Array[String],
      handleAddition: js.Function1[String, Unit],
      handleDelete: js.Function1[Int, Unit],
      handleDrag: js.Function3[TagObject, Int, Int, Unit],
      delimiters: js.Array[Int],
      minQueryLength: Int,
      classNames: js.Dictionary[String],
      autoFocus: Boolean,
  ) {
    def toJsObject: js.Object =
      js.Dynamic.literal(
        tags = tags,
        suggestions = suggestions,
        handleAddition = handleAddition,
        handleDelete = handleDelete,
        handleDrag = handleDrag,
        delimiters = delimiters,
        minQueryLength = minQueryLength,
        classNames = classNames,
        autofocus = autoFocus,
      )
  }
}

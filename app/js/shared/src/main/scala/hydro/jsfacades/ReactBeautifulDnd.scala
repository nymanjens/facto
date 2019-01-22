package hydro.jsfacades

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.Children
import japgolly.scalajs.react.JsComponent
import japgolly.scalajs.react.raw
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ReactBeautifulDnd {

  // **************** API ****************//
  def DragDropContext(onDragEndHandler: OnDragEndHandler)(children: VdomNode*) = {
    val onDragEnd: js.Function1[Result, Unit] = result => {
      onDragEndHandler(
        sourceIndex = result.source.index,
        destinationIndex = Option(result.destination) map (_.index))
    }

    val component = JsComponent[js.Object, Children.Varargs, Null](DragDropContextComponent)
    component(js.Dynamic.literal(onDragEnd = onDragEnd))(children: _*)
  }

  def Droppable(droppableId: String)(children: (Provided, Snapshot) => VdomNode) = {
    val jsChildren: js.Function2[Provided, Snapshot, raw.React.Element] =
      (a, b) => <.span(children(a, b)).rawElement

    val component = JsComponent[js.Object, Children.None, Null](DroppableComponent)
    component(js.Dynamic.literal(droppableId = droppableId, children = jsChildren))
  }

  def Draggable(key: Any, draggableId: String, index: Int)(children: (Provided, Snapshot) => VdomNode) = {
    val jsChildren: js.Function2[Provided, Snapshot, raw.React.Element] =
      (a, b) => <.span(children(a, b)).rawElement

    val component = JsComponent[js.Object, Children.None, Null](DraggableComponent)
    component(
      js.Dynamic.literal(key = key.toString, draggableId = draggableId, index = index, children = jsChildren))
  }

  // **************** Public inner types ****************//
  trait OnDragEndHandler {
    def apply(sourceIndex: Int, destinationIndex: Option[Int]): Unit
  }

  @js.native
  trait Provided extends js.Object {
    def innerRef: js.Object = js.native
    def draggableProps: js.Dictionary[js.Object] = js.native
    def dragHandleProps: js.Dictionary[js.Object] = js.native
  }
  @js.native
  trait Snapshot extends js.Object {
    def isDragging: Boolean = js.native
    def isDraggingOver: Boolean = js.native
  }

  // **************** Private inner types ****************//
  @JSImport("react-beautiful-dnd", "DragDropContext")
  @js.native
  private object DragDropContextComponent extends js.Object

  @JSImport("react-beautiful-dnd", "Droppable")
  @js.native
  private object DroppableComponent extends js.Object

  @JSImport("react-beautiful-dnd", "Draggable")
  @js.native
  private object DraggableComponent extends js.Object

  @js.native
  private trait Result extends js.Object {
    def source: Item = js.native
    def destination: Item = js.native
  }

  @js.native
  private trait Item extends js.Object {
    def index: Int = js.native
  }
}

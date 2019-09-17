package hydro.common.testing

import hydro.common.DomNodeUtils
import japgolly.scalajs.react._
import japgolly.scalajs.react.test.ReactTestUtils.MountedOutput
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.test.Simulate
import japgolly.scalajs.react.vdom.VdomElement
import org.scalajs.dom.raw

import scala.collection.immutable.Seq
import scala.scalajs.js

final class ReactTestWrapper(private val element: raw.Element) {

  /** Will return all children that comply to both the tagName, class and type filter (active if non-empty). */
  def children(tagName: String = "", clazz: String = "", tpe: String = ""): Seq[ReactTestWrapper] = {
    for {
      nodeWithOffset <- DomNodeUtils.walkDepthFirstPreOrder(element).toVector
      element <- DomNodeUtils.asElement(nodeWithOffset.node)
      child <- Some(new ReactTestWrapper(element))
      if tagName.isEmpty || child.tagName == tagName.toLowerCase
      if clazz.isEmpty || child.classes.contains(clazz.toLowerCase)
      if tpe.isEmpty || child.typeAttribute == tpe.toLowerCase
    } yield child
  }

  def child(tagName: String = "", clazz: String = "", tpe: String = ""): ReactTestWrapper = {
    maybeChild(tagName, clazz, tpe).get
  }

  def maybeChild(tagName: String = "", clazz: String = "", tpe: String = ""): Option[ReactTestWrapper] = {
    val childList = children(tagName, clazz, tpe)
    childList match {
      case Nil       => None
      case Seq(elem) => Some(elem)
      case _         => throw new MatchError(childList)
    }
  }

  def attribute(name: String): String = {
    Option(element.getAttribute(name)) getOrElse ""
  }

  def click(): Unit = {
    Simulate.click(element)
  }

  def typeKeyboardCharacter(
      key: String,
      shiftKey: Boolean = false,
      altKey: Boolean = false,
      ctrlKey: Boolean = false,
  ): Unit = {
    val eventData = js.Dynamic.literal(key = key, shiftKey = shiftKey, altKey = altKey, ctrlKey = ctrlKey)
    Simulate.keyDown(element, eventData = eventData)
    Simulate.keyPress(element, eventData = eventData)
    Simulate.keyUp(element, eventData = eventData)
  }

  def classes: Seq[String] = {
    val classString = Option(element.getAttribute("class")) getOrElse ""
    classString.toLowerCase.split(' ').toVector
  }

  def tagName: String = {
    element.tagName.toLowerCase
  }

  def typeAttribute: String = {
    val attrib = Option(element.getAttribute("type")) getOrElse ""
    attrib.toLowerCase
  }
}

object ReactTestWrapper {
  def renderComponent(component: VdomElement): ReactTestWrapper = {
    val componentM: MountedOutput = ReactTestUtils renderIntoDocument component
    new ReactTestWrapper(componentM.getDOMNode.asElement)
  }
}

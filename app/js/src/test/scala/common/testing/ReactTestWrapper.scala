package common.testing

import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.test.ReactTestUtils.MountedOutput
import japgolly.scalajs.react.test.raw.ReactAddonsTestUtils
import japgolly.scalajs.react.vdom.VdomElement

import scala.collection.immutable.Seq

final class ReactTestWrapper(private val componentM: MountedOutput) {

  /** Will return all children that comply to both the tagName, class and type filter (active if non-empty). */
  def children(tagName: String = "", clazz: String = "", tpe: String = ""): Seq[ReactTestWrapper] = {
    val childComponentMs = ReactTestUtils.findAllInRenderedTree(
      componentM,
      childComponent => {
        val child = new ReactTestWrapper(childComponent)
        (clazz.isEmpty || child.classes.contains(clazz.toLowerCase)) &&
        (tagName.isEmpty || child.tagName == tagName.toLowerCase) &&
        (tpe.isEmpty || child.typeAttribute == tpe.toLowerCase)
      }
    )
    childComponentMs.map(new ReactTestWrapper(_))
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
    Option(componentM.getDOMNode.getAttribute(name)) getOrElse ""
  }

  def click(): Unit = {
    ReactAddonsTestUtils.Simulate.click(componentM.getDOMNode)
  }

  def classes: Seq[String] = {
    val classString = Option(componentM.getDOMNode.getAttribute("class")) getOrElse ""
    classString.toLowerCase.split(' ').toVector
  }

  def tagName: String = {
    componentM.getDOMNode.tagName.toLowerCase
  }

  def typeAttribute: String = {
    val attrib = Option(componentM.getDOMNode.getAttribute("type")) getOrElse ""
    attrib.toLowerCase
  }
}

object ReactTestWrapper {
  def renderComponent(component: VdomElement): ReactTestWrapper = {
    new ReactTestWrapper(ReactTestUtils renderIntoDocument component)
  }
}

package common.testing

import japgolly.scalajs.react.VdomElement
import japgolly.scalajs.react.test.{ComponentM, ReactTestUtils}
import org.scalajs.dom.raw.DOMList
import scala.scalajs.js.{Function1, isUndefined}

import scala.collection.immutable.Seq

final class ReactTestWrapper(private val componentM: ComponentM) {

  /** Will return all children that comply to both the tagName, class and type filter (active if non-empty). */
  def children(tagName: String = "", clazz: String = "", tpe: String = ""): Seq[ReactTestWrapper] = {
    val childComponentMs = ReactTestUtils.findAllInRenderedTree(
      componentM,
      toJsFunction(childComponent => {
        val child = new ReactTestWrapper(childComponent)
        (clazz.isEmpty || child.classes.contains(clazz.toLowerCase)) &&
        (tagName.isEmpty || child.tagName == tagName.toLowerCase) &&
        (tpe.isEmpty || child.typeAttribute == tpe.toLowerCase)
      })
    )
    childComponentMs.toVector.map(new ReactTestWrapper(_))
  }

  def child(tagName: String = "", clazz: String = "", tpe: String = ""): ReactTestWrapper = {
    maybeChild(tagName, clazz, tpe).get
  }

  def maybeChild(tagName: String = "", clazz: String = "", tpe: String = ""): Option[ReactTestWrapper] = {
    val childList = children(tagName, clazz, tpe)
    childList match {
      case Nil => None
      case Seq(elem) => Some(elem)
      case _ => throw new MatchError(childList)
    }
  }

  def attribute(name: String): String = {
    Option(componentM.getDOMNode.getAttribute(name)) getOrElse ""
  }

  def click(): Unit = {
    Simulate click componentM
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

  private def toJsFunction[T, R](f: T => R): Function1[T, R] = f
}

object ReactTestWrapper {
  def renderComponent(component: VdomElement): ReactTestWrapper = {
    new ReactTestWrapper(ReactTestUtils renderIntoDocument component)
  }
}

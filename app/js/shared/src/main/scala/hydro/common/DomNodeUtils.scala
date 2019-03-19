package hydro.common

import org.scalajs.dom

import scala.collection.immutable.Seq

object DomNodeUtils {

  def asTextNode(node: dom.raw.Node): Option[dom.raw.Text] = {
    if (node.nodeType == dom.raw.Node.TEXT_NODE) {
      Some(node.asInstanceOf[dom.raw.Text])
    } else {
      None
    }
  }

  def asElement(node: dom.raw.Node): Option[dom.raw.Element] = {
    if (node.nodeType == dom.raw.Node.ELEMENT_NODE) {
      Some(node.asInstanceOf[dom.raw.Element])
    } else {
      None
    }
  }
  def nodeIsList(node: dom.raw.Node): Boolean = parseNode(node) match {
    case ParsedNode.Ul(_) | ParsedNode.Ol(_) => true
    case _                                   => false
  }
  def nodeIsLi(node: dom.raw.Node): Boolean = parseNode(node).isInstanceOf[ParsedNode.Li]

  def children(node: dom.raw.Node): Seq[dom.raw.Node] = {
    for (i <- 0 until node.childNodes.length) yield node.childNodes.item(i)
  }

  def walkDepthFirstPreOrder(node: dom.raw.Node): Iterable[NodeWithOffset] = {
    var offsetSoFar = 0
    def internal(node: dom.raw.Node): Iterable[NodeWithOffset] = {
      val nodeLength = asTextNode(node).map(_.length) getOrElse 0
      val nodeWithOffset = NodeWithOffset(node, offsetSoFar, offsetAtEnd = offsetSoFar + nodeLength)
      offsetSoFar += nodeLength

      nodeWithOffset +: children(node).flatMap(internal)
    }
    internal(node)
  }

  case class NodeWithOffset(node: dom.raw.Node, offsetSoFar: Int, offsetAtEnd: Int)

  def parseNode(node: dom.raw.Node): ParsedNode = {
    if (node.nodeType == dom.raw.Node.TEXT_NODE) {
      ParsedNode.Text(node.asInstanceOf[dom.raw.Text].wholeText)
    } else if (node.nodeType == dom.raw.Node.ELEMENT_NODE) {
      val element = node.asInstanceOf[dom.raw.Element]
      element.tagName match {
        case "LI"    => ParsedNode.Li(element)
        case "UL"    => ParsedNode.Ul(element)
        case "OL"    => ParsedNode.Ol(element)
        case "BR"    => ParsedNode.Br(element)
        case "DIV"   => ParsedNode.Div(element)
        case "P"     => ParsedNode.P(element)
        case "B"     => ParsedNode.B(element)
        case "I"     => ParsedNode.I(element)
        case "S"     => ParsedNode.S(element)
        case "CODE"  => ParsedNode.Code(element)
        case "A"     => ParsedNode.A(element)
        case "STYLE" => ParsedNode.Style(element)
        case _       => ParsedNode.Other(node)
      }
    } else {
      ParsedNode.Other(node)
    }
  }

  sealed trait ParsedNode
  object ParsedNode {
    case class Text(string: String) extends ParsedNode
    case class Li(element: dom.raw.Element) extends ParsedNode
    case class Ul(element: dom.raw.Element) extends ParsedNode
    case class Ol(element: dom.raw.Element) extends ParsedNode
    case class Br(element: dom.raw.Element) extends ParsedNode
    case class Div(element: dom.raw.Element) extends ParsedNode
    case class P(element: dom.raw.Element) extends ParsedNode
    case class B(element: dom.raw.Element) extends ParsedNode
    case class I(element: dom.raw.Element) extends ParsedNode
    case class S(element: dom.raw.Element) extends ParsedNode
    case class Code(element: dom.raw.Element) extends ParsedNode
    case class A(element: dom.raw.Element) extends ParsedNode
    case class Style(element: dom.raw.Element) extends ParsedNode
    case class Other(node: dom.raw.Node) extends ParsedNode
  }
}

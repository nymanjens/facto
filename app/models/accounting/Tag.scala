package models.accounting

import scala.util.matching.Regex

case class Tag(name: String)

object Tag {
  private val validTagRegex: Regex = """[a-zA-Z0-9-_@$&()+=!.<>;:]+""".r

  def isValidTagName(name: String): Boolean = name match {
    case validTagRegex() => true
    case _ => false
  }
}

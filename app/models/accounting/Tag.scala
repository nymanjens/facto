package models.accounting

import com.google.common.hash.Hashing

import math.abs
import scala.collection.immutable.Seq
import scala.util.matching.Regex

case class Tag(name: String) {
  /** Returns the Bootstrap label class for this Tag. **/
  def labelClass: String = {
    val hash = Hashing.sha1().hashUnencodedChars(name).asInt()
    val index = abs(hash) % Tag.labelClassOptions.size
    Tag.labelClassOptions(index)
  }
}

object Tag {
  private val validTagRegex: Regex = """[a-zA-Z0-9-_@$&()+=!.<>;:]+""".r
  private val labelClassOptions: Seq[String] =
    Seq("label-primary", "label-success", "label-info", "label-warning", "label-danger")

  def isValidTagName(name: String): Boolean = name match {
    case validTagRegex() => true
    case _ => false
  }
}

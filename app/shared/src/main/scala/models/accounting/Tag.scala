package models.accounting

import scala.collection.immutable.Seq
import scala.math.abs
import scala.util.matching.Regex

case class Tag(name: String) {
  /** Returns the Bootstrap label class for this Tag. **/
  def bootstrapClassSuffix: String = {
    val index = abs(name.hashCode) % Tag.bootstrapClassSuffixOptions.size
    Tag.bootstrapClassSuffixOptions(index)
  }
}

object Tag {
  private val validTagRegex: Regex = """[a-zA-Z0-9-_@$&()+=!.<>;:]+""".r
  private val bootstrapClassSuffixOptions: Seq[String] =
    Seq("primary", "success", "info", "warning", "danger")

  def isValidTagName(name: String): Boolean = name match {
    case validTagRegex() => true
    case _ => false
  }

  def parseTagsString(tagsString: String): Seq[Tag] = {
    tagsString.split(",").map(_.trim).filter(_.nonEmpty).map(Tag.apply).toVector
  }

  def serializeToString(tags: Iterable[Tag]): String = tags.mkString(",")
}

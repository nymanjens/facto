package hydro.common

import hydro.common.GuavaReplacement.Splitter

import scala.collection.immutable.Seq
import scala.math.abs
import scala.util.matching.Regex

object Tags {
  private val validTagRegex: Regex = """[a-zA-Z0-9-_@$&()+=!.<>;: ]+""".r

  def isValidTag(tag: String): Boolean = tag match {
    case validTagRegex() => true
    case _               => false
  }

  /** Parse a comma-separated list of tags that are assumed to be validated already. */
  def parseTagsString(tagsString: String): Seq[String] = {
    Splitter.on(',').omitEmptyStrings().trimResults().split(tagsString)
  }

  def serializeToString(tags: Iterable[String]): String = tags.mkString(",")
}

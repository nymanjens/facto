package models.accounting

import com.google.common.base.{Charsets, Splitter}
import com.google.common.hash.Hashing

import math.abs
import scala.collection.immutable.Seq
import scala.util.matching.Regex
import scala.collection.JavaConverters._

case class Tag(name: String) {
  /** Returns the Bootstrap label class for this Tag. **/
  def bootstrapClassSuffix: String = {
    val hashString: String = Hashing.sha1().hashString(name, Charsets.UTF_8).toString()
    val hashedInt: Int = {
      var hash: Int = 0
      for (char <- hashString) {
        hash = ((hash << 5) - hash) + char;
      }
      hash
    }
    val index = abs(hashedInt) % Tag.bootstrapClassSuffixOptions.size
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
    Splitter.on(",").omitEmptyStrings().split(tagsString).asScala.map(Tag.apply).toVector
  }
}

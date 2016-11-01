package viewhelpers

import scala.collection.mutable
import scala.collection.Set
import scala.annotation.tailrec
import collection.JavaConversions._
import com.google.common.collect.Sets
import java.text.Normalizer

object IdGenerator {

  private val reservedIds: mutable.Set[String] = Sets.newConcurrentHashSet[String]()

  /**
    * Generates a unique string id, suitable for use as HTML tag ID. The suggestion string may contain any characters,
    * but the result will not necessarily contain the suggestion.
    */
  def uniqueId(suggestion: String): String = {
    val uniqueId = generateUniqueSlug(slugify(suggestion), reservedIds)
    reservedIds += uniqueId
    uniqueId
  }

  // source: https://github.com/julienrf/chooze/blob/master/app/util/Util.scala
  private def slugify(str: String): String = {
    Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\w ]", "").replace(" ", "-").toLowerCase
  }

  // source: https://github.com/julienrf/chooze/blob/master/app/util/Util.scala
  @tailrec
  private def generateUniqueSlug(slug: String, existingSlugs: Set[String]): String = {
    if (!(existingSlugs contains slug)) {
      slug
    } else {
      val EndsWithNumber = "(.+-)([0-9]+)$".r
      slug match {
        case EndsWithNumber(s, n) => generateUniqueSlug(s + (n.toInt + 1), existingSlugs)
        case s => generateUniqueSlug(s + "-2", existingSlugs)
      }
    }
  }
}

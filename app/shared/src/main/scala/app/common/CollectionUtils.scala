package app.common

import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

object CollectionUtils {

  /** Converts list of pairs to ListMap. **/
  def toListMap[A, B](entries: Iterable[(A, B)]): ListMap[A, B] = ListMap(entries.toSeq: _*)

  def asMap[K, V](keys: Iterable[K], valueFunc: K => V): Map[K, V] = keys.map(k => k -> valueFunc(k)).toMap

  def getMostCommonStringIgnoringCase(strings: Iterable[String]): String = {
    require(strings.nonEmpty)
    val mostCommonLowerCaseString = getMostCommonString(strings.map(_.toLowerCase))
    getMostCommonString(strings.filter(_.toLowerCase == mostCommonLowerCaseString))
  }

  def getMostCommonString(strings: Iterable[String]): String = {
    strings.groupBy(identity).mapValues(_.size).toSeq.minBy(-_._2)._1
  }

  def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()
}

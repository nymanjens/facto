package hydro.common

import hydro.common.GuavaReplacement.ImmutableBiMap

import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq

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

  def maybeGet[E](seq: Seq[E], index: Int): Option[E] = {
    if (seq.indices contains index) {
      Some(seq(index))
    } else {
      None
    }
  }

  /**
    * Converts the given values to a bimap that associates an integer with each value.
    *
    * These associated integers remain stable as long as the `stableNameMapper` returns the same value,
    * even if the order of values is changed, or values are added/removed.
    */
  def toBiMapWithStableIntKeys[V](
      stableNameMapper: V => String,
      values: Iterable[V],
  ): ImmutableBiMap[V, Int] = {
    val valuesSeq = values.toVector
    val names = valuesSeq.map(stableNameMapper)
    val hashCodes = names.map(_.hashCode)

    require(names.distinct.size == names.size, s"There are names that are not unique: $names")
    require(
      hashCodes.distinct.size == hashCodes.size,
      s"There are hash codes that are not unique: $hashCodes. " +
        "This is bad luck and can be solved by adding a salt (missing feature at the moment)."
    )

    val resultBuilder = ImmutableBiMap.builder[V, Int]()
    for ((value, hash) <- valuesSeq zip hashCodes) {
      resultBuilder.put(value, hash)
    }
    resultBuilder.build()
  }
}

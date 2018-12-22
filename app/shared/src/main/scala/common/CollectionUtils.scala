package common

import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

object CollectionUtils {

  /** Converts list of pairs to ListMap. **/
  def toListMap[A, B](entries: Iterable[(A, B)]): ListMap[A, B] = ListMap(entries.toSeq: _*)

  def asMap[K, V](keys: Iterable[K], valueFunc: K => V): Map[K, V] = keys.map(k => k -> valueFunc(k)).toMap

  def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()
}

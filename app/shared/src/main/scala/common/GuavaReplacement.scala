package common

import scala.collection.immutable.Seq
import scala.collection.mutable

/**
  * Replaces some Guava-provided functionality that is no longer usable with scala.js.
  */
object GuavaReplacement {

  object Iterables {
    def getOnlyElement[T](traversable: Traversable[T]): T = {
      val list = traversable.toList
      require(list.size == 1, s"Given traversable can only have one element but is $list")
      list.head
    }
  }

  object DoubleMath {
    def roundToLong(x: Double): Long = {
      Math.round(x)
    }
  }

  object Preconditions {
    def checkNotNull[T](value: T): T = {
      if (value == null) {
        throw new NullPointerException()
      }
      value
    }
  }

  final class ImmutableSetMultimap[A, B](private val backingMap: Map[A, Set[B]]) {
    def get(key: A): Set[B] = backingMap.getOrElse(key, Set())
    def keySet: Set[A] = backingMap.keySet

    override def toString = backingMap.toString

    override def equals(that: scala.Any) = that match {
      case that: ImmutableSetMultimap[A, B] => backingMap == that.backingMap
      case _ => false
    }
    override def hashCode() = backingMap.hashCode()
  }
  object ImmutableSetMultimap {
    def builder[A, B](): Builder[A, B] = new Builder[A, B]()
    def of[A, B](): ImmutableSetMultimap[A, B] = new Builder[A, B]().build()

    final class Builder[A, B] private[ImmutableSetMultimap] () {
      private val backingMap = mutable.Map[A, Set[B]]()

      def put(key: A, value: B): Builder[A, B] = {
        val existingList = backingMap.getOrElse(key, Set())
        backingMap.put(key, existingList + value)
        this
      }
      def putAll(key: A, values: B*): Builder[A, B] = {
        val existingList = backingMap.getOrElse(key, Set())
        backingMap.put(key, existingList ++ values)
        this
      }

      def build(): ImmutableSetMultimap[A, B] = new ImmutableSetMultimap(backingMap.toMap)
    }
  }
}

package hydro.common

import java.time.Duration

import scala.collection.immutable.Seq
import scala.collection.mutable

/** Replaces some Guava-provided functionality that is not usable with scala.js. */
object GuavaReplacement {

  // **************** Iterables **************** //
  object Iterables {
    def getOnlyElement[T](traversable: Traversable[T]): T = {
      val list = traversable.toList
      require(list.size == 1, s"Given traversable can only have one element but is $list")
      list.head
    }
  }

  // **************** DoubleMath **************** //
  object DoubleMath {
    def roundToLong(x: Double): Long = {
      Math.round(x)
    }
  }

  // **************** Preconditions **************** //
  object Preconditions {
    def checkNotNull[T](value: T): T = {
      if (value == null) {
        throw new NullPointerException()
      }
      value
    }
  }

  // **************** Splitter **************** //
  final class Splitter(separator: Char) {
    private var _omitEmptyStrings: Boolean = false
    private var _trimResults: Boolean = false

    def omitEmptyStrings(): this.type = {
      _omitEmptyStrings = true
      this
    }

    def trimResults(): this.type = {
      _trimResults = true
      this
    }

    def split(string: String): Seq[String] = {
      val parts = mutable.Buffer[String]()
      val nextPart = new StringBuilder

      for (char <- string) char match {
        case `separator` =>
          parts += nextPart.result()
          nextPart.clear()
        case _ =>
          nextPart += char
      }
      parts += nextPart.result()
      var result = Seq(parts: _*)
      if (_trimResults) {
        result = result.map(_.trim)
      }
      if (_omitEmptyStrings) {
        result = result.filter(_.nonEmpty)
      }
      result
    }
  }
  object Splitter {
    def on(separator: Char): Splitter = new Splitter(separator)
  }

  // **************** Stopwatch **************** //
  final class Stopwatch private () {
    private var startTimeMillis = System.currentTimeMillis

    def elapsed: Duration = {
      val nowMillis = System.currentTimeMillis
      Duration.ofMillis(nowMillis - startTimeMillis)
    }

    def reset(): Unit = {
      startTimeMillis = System.currentTimeMillis
    }
  }

  object Stopwatch {
    def createStarted(): Stopwatch = new Stopwatch()
  }

  // **************** Multimap **************** //
  final class ImmutableSetMultimap[A, B](private val backingMap: Map[A, Set[B]]) {
    def get(key: A): Set[B] = backingMap.getOrElse(key, Set())
    def keySet: Set[A] = backingMap.keySet
    def containsValue(value: B): Boolean = backingMap.values.toStream.flatten.contains(value)
    def values: Iterable[B] = backingMap.values.toStream.flatMap(set => set)

    def toBuilder: ImmutableSetMultimap.Builder[A, B] = {
      val builder = ImmutableSetMultimap.builder[A, B]()
      for ((k, v) <- backingMap) {
        builder.putAll(k, v.toSeq: _*)
      }
      builder
    }

    override def toString = backingMap.toString

    override def equals(that: scala.Any) = that match {
      case that: ImmutableSetMultimap[A, B] => backingMap == that.backingMap
      case _                                => false
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

  final class ImmutableBiMap[K, V](private val forwardMap: Map[K, V], private val backwardMap: Map[V, K]) {
    def get(key: K): V = forwardMap(key)
    def inverse(): ImmutableBiMap[V, K] = new ImmutableBiMap(backwardMap, forwardMap)
    def keySet: Set[K] = forwardMap.keySet
    override def toString: String = forwardMap.toString
    override def equals(o: Any): Boolean = o match {
      case that: ImmutableBiMap[_, _] => this.forwardMap == that.forwardMap
      case _                          => false
    }
    override def hashCode(): Int = forwardMap.hashCode()
  }
  object ImmutableBiMap {
    def builder[K, V](): Builder[K, V] = new Builder[K, V]()
    def of[K, V](): ImmutableBiMap[K, V] = new Builder[K, V]().build()

    final class Builder[K, V] private[ImmutableBiMap] () {
      private val forwardMap = mutable.Map[K, V]()
      private val backwardMap = mutable.Map[V, K]()

      def put(key: K, value: V): Builder[K, V] = {
        require(!forwardMap.contains(key), s"key $key already exists in keySet ${forwardMap.keySet}")
        require(
          !backwardMap.contains(value),
          s"value $value already exists in valueSet ${backwardMap.keySet}")
        forwardMap.put(key, value)
        backwardMap.put(value, key)
        this
      }

      def build(): ImmutableBiMap[K, V] = new ImmutableBiMap(forwardMap.toMap, backwardMap.toMap)
    }
  }

  // **************** LoadingCache **************** //
  final class LoadingCache[K, V](loader: K => V) {
    private val backingMap: mutable.Map[K, V] = mutable.Map()

    def get(key: K): V = this.synchronized {
      if (backingMap contains key) {
        backingMap(key)
      } else {
        val value = loader(key)
        backingMap.put(key, value)
        value
      }
    }

    def asMap(): Map[K, V] = this.synchronized {
      backingMap.toMap
    }
  }

  object LoadingCache {
    def fromLoader[K, V](loader: K => V): LoadingCache[K, V] = new LoadingCache(loader)
  }
}

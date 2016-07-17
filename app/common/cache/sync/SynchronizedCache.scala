package common.cache.sync

import org.joda.time.Duration

/**
  * Defines a cache that acts similar to a Guava cache, except that the client can iterate over every entry while
  * holding a global lock.
  */
trait SynchronizedCache[K <: Object, V <: Object] {

  /**
    * If key is in the cache, its stored value is returned. Otherwise, the given function is used to calculate and store
    * the value.
    */
  def getOrCalculate(key: K, calculateValueFunc: () => V): V

  /** Removes key from the cache. */
  def invalidate(key: K): Unit

  /**
    * Iterates over every entry while holding a global lock. During iteration, all other operations are postponed until
    * this iteration finishes.
    */
  def foreachWithLock(f: V => Unit): Unit
}

object SynchronizedCache {

  def apply[K <: Object, V <: Object](expireAfterAccess: Duration = Duration.standardDays(99999),
                                      maximumSize: Long = Long.MaxValue): SynchronizedCache[K, V] = {
    new GuavaBackedSynchronizedCache[K, V](expireAfterAccess, maximumSize)
  }
}

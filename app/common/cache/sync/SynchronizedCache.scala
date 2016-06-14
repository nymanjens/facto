package common.cache.sync

import com.google.common.hash.HashCode
import common.cache.UniquelyHashable
import org.joda.time.Duration

trait SynchronizedCache[K <: Object, V <: Object] {
  def getOrCalculate(key: K, calculateValueFunc: () => V): V
  def invalidate(key: K): Unit
  def foreachWithLock(f: V => Unit): Unit
}

object SynchronizedCache {
  def apply[K <: Object, V <: Object](expireAfterAccess: Duration = Duration.standardDays(99999),
                                      maximumSize: Long = Long.MaxValue): SynchronizedCache[K, V] = {
    new GuavaBackedSynchronizedCache[K, V](expireAfterAccess, maximumSize)
  }

  def hashingKeys[K <: UniquelyHashable, V <: Object](expireAfterAccess: Duration = Duration.standardDays(99999),
                                                      maximumSize: Long = Long.MaxValue): SynchronizedCache[K, V] = {
    new KeyHashingSynchronizedCache[K, V](SynchronizedCache[HashCode, V](expireAfterAccess, maximumSize))
  }
}

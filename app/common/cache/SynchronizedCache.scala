package common.cache

import scala.collection.immutable.Seq
import scala.collection.mutable

import java.util.function.BiConsumer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.Callable

import org.joda.time.Duration
import org.apache.http.annotation.GuardedBy
import com.google.common.cache.{Cache, CacheBuilder, LoadingCache, CacheLoader}

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
}

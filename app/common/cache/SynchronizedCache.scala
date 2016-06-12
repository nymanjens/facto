package common.cache

import scala.collection.immutable.Seq
import scala.collection.mutable

import java.util.function.BiConsumer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.Callable

import org.joda.time.Period
import org.apache.http.annotation.GuardedBy
import com.google.common.cache.{Cache, CacheBuilder, LoadingCache, CacheLoader}


class SynchronizedCache[K <: Object, V <: Object](expireAfterAccess: Period = Period.years(99),
                                                  maximumSize: Long = Long.MaxValue) {
  CacheMaintenanceManager.registerCache(doMaintenance = () => guavaCache.cleanUp())

  @GuardedBy("lock (all reads and writes)")
  private val guavaCache: Cache[K, V] = CacheBuilder.newBuilder()
    .maximumSize(maximumSize)
    .expireAfterAccess(expireAfterAccess.getSeconds, SECONDS)
    .build[K, V]()

  private val lock = new Object

  def getOrCalculate(key: K, calculateValueFunc: () => V): V = {
    val currentValue = lock synchronized {
      Option(guavaCache.getIfPresent(key))
    }

    if (currentValue.isDefined) {
      currentValue.get
    } else {
      // Key not yet cached. Calculate outside of lock and put in cache.
      val value = calculateValueFunc()
      lock synchronized {
        // Not using put because there might already be a value by now.
        guavaCache.get(key, new Callable[V]() {
          override def call = value
        })
      }
    }
  }

  def invalidate(key: K): Unit = lock synchronized {
    guavaCache.invalidate(key)
  }

  def foreachWithLock(f: V => Unit): Unit = lock synchronized {
    guavaCache.asMap().forEach(new BiConsumer[K, V]() {
      override def accept(key: K, value: V) = f(value)
    })
  }
}

package common.cache.sync

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.function.BiConsumer

import com.google.common.cache.{Cache, CacheBuilder}
import common.cache.CacheRegistry
import org.apache.http.annotation.GuardedBy
import org.joda.time.Duration

private[sync] final class GuavaBackedSynchronizedCache[K <: Object, V <: Object](expireAfterAccess: Duration, maximumSize: Long)
  extends SynchronizedCache[K, V] {
  CacheRegistry.registerCache(doMaintenance = () => guavaCache.cleanUp())

  @GuardedBy("lock (all reads and writes)")
  private val guavaCache: Cache[K, V] = CacheBuilder.newBuilder()
    .maximumSize(maximumSize)
    .expireAfterAccess(expireAfterAccess.getMillis, MILLISECONDS)
    .build[K, V]()

  private val lock = new Object

  override def getOrCalculate(key: K, calculateValueFunc: () => V): V = {
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

  override def invalidate(key: K): Unit = lock synchronized {
    guavaCache.invalidate(key)
  }

  override def foreachWithLock(f: V => Unit): Unit = lock synchronized {
    guavaCache.asMap().forEach(new BiConsumer[K, V]() {
      override def accept(key: K, value: V) = f(value)
    })
  }
}

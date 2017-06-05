package common.cache.sync

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.function.BiConsumer

import com.google.common.cache.{Cache, CacheBuilder}
import common.cache.CacheRegistry
import net.jcip.annotations.GuardedBy
import java.time.Duration

private[sync] final class GuavaBackedSynchronizedCache[K <: Object, V <: Object](expireAfterAccess: Duration,
                                                                                 maximumSize: Long)
    extends SynchronizedCache[K, V] {
  CacheRegistry.registerCache(
    doMaintenance = () => guavaCache.cleanUp(),
    resetForTests = () => guavaCache.invalidateAll())

  @GuardedBy("lock (all reads and writes)")
  private val guavaCache: Cache[K, V] = CacheBuilder
    .newBuilder()
    .maximumSize(maximumSize)
    .expireAfterAccess(expireAfterAccess.getNano, NANOSECONDS)
    .build[K, V]()

  private val lock = new Object

  override def getOrCalculate(key: K, calculateValueFunc: () => V): V = {
    lock synchronized {
      val currentValue = Option(guavaCache.getIfPresent(key))

      if (currentValue.isDefined) {
        currentValue.get
      } else {
        val value = calculateValueFunc()
        guavaCache.put(key, value)
        value
      }
    }
  }

  override def invalidate(key: K): Unit = lock synchronized {
    guavaCache.invalidate(key)
  }

  override def foreachWithLock(f: V => Unit): Unit = lock synchronized {
    guavaCache
      .asMap()
      .forEach(new BiConsumer[K, V]() {
        override def accept(key: K, value: V) = f(value)
      })
  }
}

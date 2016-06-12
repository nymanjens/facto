package common.cache

import scala.collection.immutable.Seq
import scala.collection.mutable

import java.util.function.BiConsumer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.Callable

import org.joda.time.Duration
import org.apache.http.annotation.GuardedBy
import com.google.common.cache.{Cache, CacheBuilder, LoadingCache, CacheLoader}

class KeyHashingSynchronizedCache[K <: UniquelyHashable, V <: Object](delegate: SynchronizedCache[String, V])
  extends SynchronizedCache[K, V] {

  override def getOrCalculate(key: K, calculateValueFunc: () => V): V =
    delegate.getOrCalculate(key.uniqueHash, calculateValueFunc)

  override def invalidate(key: K): Unit =
    delegate.invalidate(key.uniqueHash)

  override def foreachWithLock(f: (V) => Unit): Unit =
    delegate.foreachWithLock(f)
}

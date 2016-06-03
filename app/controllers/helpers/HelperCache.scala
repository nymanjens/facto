package controllers.helpers

import scala.collection.mutable

import org.apache.http.annotation.GuardedBy

import models.manager.Identifiable

object HelperCache {
  trait CacheIdentifier {
    def invalidateWhenUpdating(entity: Identifiable[_]): Boolean
  }

  @GuardedBy("lock")
  private val cache: mutable.Map[CacheIdentifier, CacheEntry[_]] = mutable.Map[CacheIdentifier, CacheEntry[_]]()
  private val lock = new Object

  def cached[R](identifier: CacheIdentifier)(expensiveFunction: () => R) = lock.synchronized {
    if (!cache.contains(identifier)) {
      cache.put(identifier, CacheEntry(expensiveFunction))
    }
    cache(identifier).value.asInstanceOf[R]
  }

  def invalidateCache(entity: Identifiable[_]): Unit = lock.synchronized {
    for ((identifier, entry) <- cache) {
      if (identifier.invalidateWhenUpdating(entity)) {
        cache.put(identifier, entry.recalculated())
      }
    }
  }

  private case class CacheEntry[R](value: R, expensiveFunction: () => R) {
    def recalculated(): CacheEntry[R] = CacheEntry(expensiveFunction)
  }

  private object CacheEntry {
    def apply[R](expensiveFunction: () => R): CacheEntry[R] = {
      CacheEntry(expensiveFunction(), expensiveFunction)
    }
  }
}
package controllers.helpers

import scala.collection.mutable

import org.apache.http.annotation.GuardedBy

import models.manager.Identifiable

object HelperCache {
  trait CacheIdentifier {
    def invalidateWhenUpdating: PartialFunction[Any, Boolean]

    private[helpers] def safeInvalidateWhenUpdating(entity: Identifiable[_]): Boolean = {
      invalidateWhenUpdating.lift(entity) getOrElse false
    }
  }

  @GuardedBy("lock")
  private val cache: mutable.Map[CacheIdentifier, CacheEntry[_]] = mutable.Map[CacheIdentifier, CacheEntry[_]]()
  private val lock = new Object

  def cached[R](identifier: CacheIdentifier)(expensiveValue: => R): R = lock.synchronized {
    val expensiveFunction = () => expensiveValue
    if (!cache.contains(identifier)) {
      cache.put(identifier, CacheEntry(expensiveFunction))
    }
    cache(identifier).value.asInstanceOf[R]
  }

  def invalidateCache(entity: Identifiable[_]): Unit = lock.synchronized {
    for ((identifier, entry) <- cache) {
      if (identifier.safeInvalidateWhenUpdating(entity)) {
        cache.remove(identifier)
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

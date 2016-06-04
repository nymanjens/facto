package controllers.helpers

import scala.collection.mutable

import org.apache.http.annotation.GuardedBy

import models.manager.Identifiable

object HelperCache {
  trait CacheIdentifier[R] {
    protected def invalidateWhenUpdating: PartialFunction[Any, Boolean] = PartialFunction.empty
    protected def invalidateWhenUpdatingEntity(oldValue: R): PartialFunction[Any, Boolean] = PartialFunction.empty

    private[helpers] def combinedInvalidateWhenUpdating(oldValue: R, entity: Identifiable[_]): Boolean = {
      val combinedInvalidate = invalidateWhenUpdating orElse invalidateWhenUpdatingEntity(oldValue)
      if (combinedInvalidate.isDefinedAt(entity)) combinedInvalidate(entity) else false
    }
  }

  @GuardedBy("lock")
  private val cache: mutable.Map[CacheIdentifier[_], CacheEntry[_]] = mutable.Map[CacheIdentifier[_], CacheEntry[_]]()
  private val lock = new Object

  def cached[R](identifier: CacheIdentifier[R])(expensiveValue: => R): R = lock.synchronized {
    val expensiveFunction = () => expensiveValue
    if (!cache.contains(identifier)) {
      cache.put(identifier, CacheEntry(identifier, expensiveFunction))
    }
    cache(identifier).value.asInstanceOf[R]
  }

  def invalidateCache(entity: Identifiable[_]): Unit = lock.synchronized {
    for ((identifier, entry) <- cache) {
      if (entry.invalidateWhenUpdating(entity)) {
        cache.remove(identifier)
      }
    }
  }

  def verifyConsistency(): Unit = lock.synchronized {
    for ((identifier, entry) <- cache) {
      val cachedValue = entry.value
      val newValue = entry.expensiveFunction()
      require(cachedValue == newValue, s"cachedValue = $cachedValue != newValue = $newValue")
    }
  }

  private case class CacheEntry[R](identifier: CacheIdentifier[R],
                                   value: R,
                                   expensiveFunction: () => R) {
    def recalculated(): CacheEntry[R] = CacheEntry(identifier, expensiveFunction)

    private[helpers] def invalidateWhenUpdating(entity: Identifiable[_]): Boolean =
      identifier.combinedInvalidateWhenUpdating(value, entity)
  }

  private object CacheEntry {
    def apply[R](identifier: CacheIdentifier[R], expensiveFunction: () => R): CacheEntry[R] = {
      CacheEntry(identifier, expensiveFunction(), expensiveFunction)
    }
  }
}

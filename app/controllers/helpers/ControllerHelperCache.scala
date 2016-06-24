package controllers.helpers

import scala.collection.immutable.Seq
import org.joda.time.Duration
import common.cache.CacheRegistry
import common.cache.sync.SynchronizedCache
import models.manager.Entity

/**
  * Key-value cache that is tailor made for caching controller helpers that fetch database entities and convert them
  * into some value.
  *
  * This cache will assume that every CacheIdentifier corresponds to a transformation from database entities to a value
  * that yields the same result until the database is updated.
  */
object ControllerHelperCache {
  CacheRegistry.registerCache(
    verifyConsistency = verifyConsistency,
    invalidateCache = invalidateCache)

  private val cache: SynchronizedCache[CacheIdentifier[_], CacheEntry[_]] =
    SynchronizedCache(expireAfterAccess = Duration.standardHours(32))

  /**
    * If given identifier is in the cache, returns the cached value. Otherwise, expensive value is calculated and
    * stored.
    *
    * Note that the value is stored only until there is a database update that invalidates the value.
    */
  def cached[R](identifier: CacheIdentifier[R])(expensiveValue: => R): R = {
    val expensiveFunction = () => expensiveValue
    val cacheEntry = cache.getOrCalculate(
      identifier,
      () => CacheEntry.calculate(identifier, expensiveFunction)
    )
    cacheEntry.value.asInstanceOf[R]
  }

  private def invalidateCache(entity: Entity[_]): Unit = {
    cache.foreachWithLock { entry =>
      if (entry.invalidateWhenUpdating(entity)) {
        cache.invalidate(entry.identifier)
      }
    }
  }

  private def verifyConsistency(): Unit = {
    cache.foreachWithLock { entry =>
      val cachedValue = entry.value
      val newValue = entry.expensiveFunction()
      require(cachedValue == newValue, s"cachedValue = $cachedValue != newValue = $newValue")
    }
  }

  /**
    * Defines an immutable value class that identifies a single cache key that should uniquely correspond to a function
    * that transforms the database entries into a value.
    *
    * Implementations must specify when the value becomes out-dated because of a database update.
    */
  trait CacheIdentifier[R] {
    protected def invalidateWhenUpdating: PartialFunction[Any, Boolean] = PartialFunction.empty
    protected def invalidateWhenUpdatingEntity(oldValue: R): PartialFunction[Any, Boolean] = PartialFunction.empty

    private[helpers] def combinedInvalidateWhenUpdating(oldValue: R, entity: Entity[_]): Boolean = {
      val combinedInvalidate = invalidateWhenUpdating orElse invalidateWhenUpdatingEntity(oldValue)
      if (combinedInvalidate.isDefinedAt(entity)) combinedInvalidate(entity) else false
    }
  }

  private case class CacheEntry[R](identifier: CacheIdentifier[R],
                                   value: R,
                                   expensiveFunction: () => R) {
    def recalculated(): CacheEntry[R] = CacheEntry.calculate(identifier, expensiveFunction)

    private[helpers] def invalidateWhenUpdating(entity: Entity[_]): Boolean =
      identifier.combinedInvalidateWhenUpdating(value, entity)
  }

  private object CacheEntry {
    def calculate[R](identifier: CacheIdentifier[R], expensiveFunction: () => R): CacheEntry[R] = {
      CacheEntry(identifier, expensiveFunction(), expensiveFunction)
    }
  }
}

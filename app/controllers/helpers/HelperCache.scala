package controllers.helpers

import scala.collection.immutable.Seq
import scala.collection.mutable

import com.google.common.cache.{Cache, CacheBuilder}
import org.apache.http.annotation.GuardedBy
import org.joda.time.Period

import common.cache.{SynchronizedCache, CacheMaintenanceManager}
import models.manager.Entity

object HelperCache {
  CacheMaintenanceManager.registerCache(
    verifyConsistency = verifyConsistency,
    invalidateCache = invalidateCache)

  private val cache: SynchronizedCache[CacheIdentifier[_], CacheEntry[_]] =
    new SynchronizedCache(expireAfterAccess = Period.hours(32))

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

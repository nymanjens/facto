package models.manager

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.Sorting

import slick.lifted.AbstractTable
import org.apache.http.annotation.GuardedBy

import common.cache.CacheRegistry

/** Caching decorator for an EntityManager that loads all data in memory and keeps it in sync with all updates. */
private[manager] final class CachingEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: EntityManager[E, T])
  extends ForwardingEntityManager[E, T](delegate) {
  CacheRegistry.registerCache(
    verifyConsistency = verifyConsistency,
    resetForTests = resetForTests)

  @GuardedBy("lock")
  private val cache: mutable.Map[Long, E] = mutable.Map[Long, E]()
  private val lock = new Object

  // ********** Implementation of EntityManager interface: Management methods ********** //
  override def initialize(): Unit = lock.synchronized {
    cache.clear()
    delegate.fetchAll() foreach { e => cache.put(e.id, e) }
  }

  // ********** Implementation of EntityManager interface: Mutators ********** //
  override def add(e: E): E = lock.synchronized {
    val result = delegate.add(e)
    cache.put(result.id, result)
    result
  }

  override def update(e: E): E = lock.synchronized {
    val result: E = delegate.update(e)
    cache.put(result.id, result)
    result
  }

  override def delete(e: E): Unit = lock.synchronized {
    delegate.delete(e)
    cache.remove(e.id)
  }

  // ********** Implementation of EntityManager interface: Getters ********** //
  override def findById(id: Long) = lock.synchronized {
    cache(id)
  }

  override def fetchAll(): List[E] = lock.synchronized {
    cache.values.toList
  }

  // ********** Private Management methods ********** //
  private def verifyConsistency(): Unit = lock.synchronized {
    val fetchedEntities = delegate.fetchAll()

    require(
      cache.size == fetchedEntities.size,
      s"cache.size = ${cache.size} must be equal to fetchedEntities.size = ${fetchedEntities.size}")
    for (fetched <- fetchedEntities) {
      val cached = cache(fetched.id)
      require(
        cached == fetched,
        s"Cached entity is not equal to fetched entity (cached = $cached, fetched = $fetched)")
    }
  }

  private def resetForTests(): Unit = lock.synchronized {
    cache.clear()
  }
}

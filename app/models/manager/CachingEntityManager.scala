package models.manager

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.Sorting

import org.apache.http.annotation.GuardedBy

private[manager] final class CachingEntityManager[E <: Identifiable[E]](delegate: EntityManager[E])
  extends ForwardingEntityManager[E](delegate) {

  @GuardedBy("lock")
  private val cache: mutable.Map[Long, E] = mutable.Map[Long, E]()
  private val lock = new Object

  // ********** Implementation of EntityManager interface: Management methods ********** //
  override def initialize(): Unit = lock.synchronized {
    delegate.fetchAll() foreach { e => cache.put(e.id, e) }
  }

  // This should always succeed
  override def verifyConsistency(): Unit = lock.synchronized {
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
  override def findById(id: Long)  = lock.synchronized {
    cache(id)
  }

  override def fetchAll(selection: Stream[E] => Stream[E]): Seq[E] = lock.synchronized {
    val stream = cache.values.toStream
    selection(stream).toVector
  }
}

//package models.manager
//
//import scala.collection.immutable.Seq
//import scala.collection.mutable
//import scala.util.Sorting
//
//import org.apache.http.annotation.GuardedBy
//
//import models.activeslick.{EntityTableQuery, Identifiable}
//
//final class CachingEntityManager[E <: Identifiable[E]](all: EntityTableQuery[E, _])
//  extends EntityManager[E] {
//
//  @GuardedBy("lock")
//  private val cache: mutable.Map[Long, E] = mutable.Map[Long, E]()
//
//  private val lock = new Object
//
//  // ********** Additional API ********** //
//  def initialize(): Unit = lock.synchronized {
//    all.fetchAll() foreach { e => cache.put(e.id.get, e) }
//  }
//
//  // This should always succeed
//  def verifyConsistency(): Unit = lock.synchronized {
//    val fetchedEntities = all.fetchAll()
//
//    require(
//      cache.size == fetchedEntities.size,
//      s"cache.size = ${cache.size} must be equal to fetchedEntities.size = ${fetchedEntities.size}")
//    for (fetched <- fetchedEntities) {
//      val cached = cache(fetched.id.get)
//      require(
//        cached == fetched,
//        s"Cached entity is not equal to fetched entity (cached = $cached, fetched = $fetched)")
//    }
//  }
//
//  // ********** Implementation of EntityManager interface ********** //
//  override def save(e: E): E = lock.synchronized {
//    val result: E = all.save(e)
//    cache.put(result.id.get, result)
//    result
//  }
//
//  override def update(e: E): E = lock.synchronized {
//    val result: E = all.update(e)
//    cache.put(result.id.get, result)
//    result
//  }
//
//  override def delete(e: E): Unit = lock.synchronized {
//    all.delete(e)
//    cache.remove(e.id.get)
//  }
//
//  override def getAll(selection: Stream[E] => Stream[E]): Seq[E] = lock.synchronized {
//    val stream = cache.values.toStream
//    selection(stream).toVector
//  }
//}
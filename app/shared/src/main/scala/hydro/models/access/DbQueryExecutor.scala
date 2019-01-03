package hydro.models.access

import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Traits for classes that can take a `DbQuery` and return their result. */
object DbQueryExecutor {

  trait Sync[E <: Entity] {
    def data(dbQuery: DbQuery[E]): Seq[E]
    def count(dbQuery: DbQuery[E]): Int

    def asAsync: Async[E] = {
      val delegate = this
      new Async[E] {
        override def data(dbQuery: DbQuery[E]) = Future.successful(delegate.data(dbQuery))
        override def count(dbQuery: DbQuery[E]) = Future.successful(delegate.count(dbQuery))
      }
    }
  }
  trait Async[E <: Entity] {
    def data(dbQuery: DbQuery[E]): Future[Seq[E]]
    def count(dbQuery: DbQuery[E]): Future[Int]
  }

  def fromEntities[E <: Entity: EntityType](entities: Iterable[E]): Sync[E] = new Sync[E] {
    override def data(dbQuery: DbQuery[E]) = stream(dbQuery).toVector
    override def count(dbQuery: DbQuery[E]) = stream(dbQuery).size

    private def stream(dbQuery: DbQuery[E]): Stream[E] = {
      var stream = entities.toStream.filter(dbQuery.filter.apply)
      for (sorting <- dbQuery.sorting) {
        stream = stream.sorted(sorting.toOrdering)
      }
      for (limit <- dbQuery.limit) {
        stream = stream.take(limit)
      }
      stream
    }
  }
}

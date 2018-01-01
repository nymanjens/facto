package models.access

import models.Entity
import models.modification.EntityType

import scala.collection.immutable.Seq
import scala.concurrent.Future

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
        stream = stream.sorted(ordering(sorting))
      }
      for (limit <- dbQuery.limit) {
        stream = stream.take(limit)
      }
      stream
    }

    private def ordering(sorting: DbQuery.Sorting[E]): Ordering[E] = (x: E, y: E) => {
      sorting.fieldsWithDirection.toStream.flatMap {
        case f @ DbQuery.Sorting.FieldWithDirection(field, isDesc) =>
          f.valueOrdering.compare(field.get(x), field.get(y)) match {
            case 0 => None
            case result if isDesc => Some(-result)
            case result => Some(result)
          }
      }.headOption getOrElse 0
    }
  }
}

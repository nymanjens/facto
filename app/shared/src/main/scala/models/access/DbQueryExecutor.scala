package models.access

import models.Entity
import models.modification.EntityType

import scala.collection.immutable.Seq
import scala.concurrent.Future

object DbQueryExecutor {

  trait Sync[E <: Entity] {
    def dataSync(dbQuery: DbQuery[E]): Seq[E]
    def countSync(dbQuery: DbQuery[E]): Int

    def asAsync: Async[E] = new Async[E] {
      override def dataAsync(dbQuery: DbQuery[E]) = Future.successful(dataSync(dbQuery))
      override def countAsync(dbQuery: DbQuery[E]) = Future.successful(countSync(dbQuery))
    }
  }
  trait Async[E <: Entity] {
    def dataAsync(dbQuery: DbQuery[E]): Future[Seq[E]]
    def countAsync(dbQuery: DbQuery[E]): Future[Int]
  }

  def fromEntities[E <: Entity: EntityType](entities: Iterable[E]): Sync[E] with Async[E] =
    new Sync[E] with Async[E] {
      override def dataSync(dbQuery: DbQuery[E]) = stream(dbQuery).toVector
      override def countSync(dbQuery: DbQuery[E]) = stream(dbQuery).size
      override def dataAsync(dbQuery: DbQuery[E]) = Future.successful(dataSync(dbQuery))
      override def countAsync(dbQuery: DbQuery[E]) = Future.successful(countSync(dbQuery))

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

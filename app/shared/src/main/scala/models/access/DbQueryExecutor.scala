package models.access

import scala.collection.immutable.Seq
import scala.concurrent.Future

trait DbQueryExecutor[E] {
  def data(dbQuery: DbQuery[E]): Future[Seq[E]]
  def count(dbQuery: DbQuery[E]): Future[Int]
}
object DbQueryExecutor {
  def fromEntities[E](entities: Iterable[E]): DbQueryExecutor[E] = new DbQueryExecutor[E] {
    override def data(dbQuery: DbQuery[E]) = Future.successful(stream(dbQuery).toVector)
    override def count(dbQuery: DbQuery[E]) = Future.successful(stream(dbQuery).size)

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

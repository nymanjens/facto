package models.access

import models.access.DbQuery.{Filter, Operation, Sorting}
import models.access.DbQueryImplicits._
import models.access.DbResultSet.DbQueryExecutor

import scala.collection.immutable.Seq
import scala.collection.mutable

final class DbResultSet[E] private (executor: DbQueryExecutor[E]) {

  private val filters: mutable.Buffer[Filter[E]] = mutable.Buffer()
  private var sorting: Option[Sorting[E]] = None
  private var limit: Option[Int] = None

  // **************** Intermediary operations **************** //
  def filter(filter: Filter[E]): DbResultSet[E] = {
    filters += filter
    this
  }

  def sort(sorting: Sorting[E]): DbResultSet[E] = {
    require(this.sorting.isEmpty, "Already added sorting")
    this.sorting = Some(sorting)
    this
  }

  def limit(quantity: Int): DbResultSet[E] = {
    require(this.limit.isEmpty, "Already added limit")
    this.limit = Some(quantity)
    this
  }

  // **************** Terminal operations **************** //
  def findOne[V](field: ModelField[V, E], value: V): Option[E] = {
    filter(field isEqualTo value).limit(1).data() match {
      case Seq(e) => Some(e)
      case Seq() => None
    }
  }
  def data(): Seq[E] = executor(dbQuery(Operation.GetDataSeq()))
  def count(): Int = executor(dbQuery(Operation.Count()))

  private def dbQuery[ReturnT](operation: Operation[E, ReturnT]): DbQuery[E, ReturnT] =
    DbQuery(
      filter = filters.toVector match {
        case Vector() => Filter.NullFilter()
        case Vector(filter) => filter
        case multipleFilters => Filter.And(multipleFilters)
      },
      sorting = sorting,
      limit = limit,
      operation = operation
    )
}

object DbResultSet {
  def fromExecutor[E](executor: DbQueryExecutor[E]): DbResultSet[E] = new DbResultSet(executor)

  trait DbQueryExecutor[E] {
    def apply[ReturnT](query: DbQuery[E, ReturnT]): ReturnT
  }
}

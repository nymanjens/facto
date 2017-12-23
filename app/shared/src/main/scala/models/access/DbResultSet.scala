package models.access

import models.Entity
import models.access.DbQuery.{Filter, Sorting}
import models.access.DbQueryImplicits._
import models.modification.EntityType

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class DbResultSet[E <: Entity: EntityType] private (executor: DbQueryExecutor[E]) {

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
  def findOne[V](field: ModelField[V, E], value: V): Future[Option[E]] = async {
    await(filter(field isEqualTo value).limit(1).data()) match {
      case Seq(e) => Some(e)
      case Seq() => None
    }
  }
  def data(): Future[Seq[E]] = executor.data(dbQuery)
  def count(): Future[Int] = executor.count(dbQuery)

  private def dbQuery: DbQuery[E] =
    DbQuery(
      filter = filters.toVector match {
        case Vector() => Filter.NullFilter()
        case Vector(filter) => filter
        case multipleFilters => Filter.And(multipleFilters)
      },
      sorting = sorting,
      limit = limit
    )
}

object DbResultSet {
  def fromExecutor[E <: Entity: EntityType](executor: DbQueryExecutor[E]): DbResultSet[E] =
    new DbResultSet(executor)
}

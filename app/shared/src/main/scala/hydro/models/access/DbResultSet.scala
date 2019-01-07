package hydro.models.access

import app.models.access.ModelFields
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery.Filter
import hydro.models.access.DbQuery.Sorting
import hydro.models.access.DbQueryImplicits._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Helper class that constructs and executes a `DbQuery` via a given `DbQueryExecutor`. */
object DbResultSet {
  def fromExecutor[E <: Entity: EntityType](executor: DbQueryExecutor.Sync[E]): Sync[E] =
    new Sync[E](executor)
  def fromExecutor[E <: Entity: EntityType](executor: DbQueryExecutor.Async[E]): Async[E] =
    new Async[E](executor)

  final class Sync[E <: Entity: EntityType] private[DbResultSet] (executor: DbQueryExecutor.Sync[E]) {

    private val helper: Helper[E] = new Helper()

    // **************** Intermediary operations **************** //
    def filter(filter: Filter[E]): Sync[E] = {
      helper.addFilter(filter)
      this
    }

    def sort(sorting: Sorting[E]): Sync[E] = {
      helper.setSorting(sorting)
      this
    }

    def limit(quantity: Int): Sync[E] = {
      helper.setLimit(quantity)
      this
    }

    // **************** Terminal operations **************** //
    def findOne[V](filter: Filter[E]): Option[E] = {
      this.filter(filter).limit(1).data() match {
        case Seq(e) => Some(e)
        case Seq()  => None
      }
    }
    def findById(id: Long): E = findOne(ModelFields.id[E] === id) match {
      case Some(x) => x
      case None    => throw new IllegalArgumentException(s"Could not find entry with id=$id")
    }
    def data(): Seq[E] = executor.data(helper.dbQuery)
    def count(): Int = executor.count(helper.dbQuery)
  }

  final class Async[E <: Entity: EntityType] private[DbResultSet] (executor: DbQueryExecutor.Async[E]) {

    private val helper: Helper[E] = new Helper()

    // **************** Intermediary operations **************** //
    def filter(filter: Filter[E]): Async[E] = {
      helper.addFilter(filter)
      this
    }

    def sort(sorting: Sorting[E]): Async[E] = {
      helper.setSorting(sorting)
      this
    }

    def limit(quantity: Int): Async[E] = {
      helper.setLimit(quantity)
      this
    }

    // **************** Terminal operations **************** //
    def findOne[V](filter: Filter[E]): Future[Option[E]] = async {
      await(this.filter(filter).limit(1).data()) match {
        case Seq(e) => Some(e)
        case Seq()  => None
      }
    }
    def findById(id: Long): Future[E] = async {
      await(findOne(ModelFields.id[E] === id)) match {
        case Some(x) => x
        case None    => throw new IllegalArgumentException(s"Could not find entry with id=$id")
      }
    }
    def data(): Future[Seq[E]] = executor.data(helper.dbQuery)
    def count(): Future[Int] = executor.count(helper.dbQuery)
  }

  private final class Helper[E <: Entity: EntityType] {
    private val filters: mutable.Buffer[Filter[E]] = mutable.Buffer()
    private var sorting: Option[Sorting[E]] = None
    private var limit: Option[Int] = None

    def addFilter(filter: Filter[E]): Unit = {
      filters += filter
    }

    def setSorting(sorting: Sorting[E]): Unit = {
      require(this.sorting.isEmpty, "Already added sorting")
      this.sorting = Some(sorting)
    }

    def setLimit(quantity: Int): Unit = {
      require(this.limit.isEmpty, "Already added limit")
      this.limit = Some(quantity)
    }

    def dbQuery: DbQuery[E] =
      DbQuery(
        filter = filters.toVector match {
          case Vector()        => Filter.NullFilter()
          case Vector(filter)  => filter
          case multipleFilters => Filter.And(multipleFilters)
        },
        sorting = sorting,
        limit = limit
      )
  }
}

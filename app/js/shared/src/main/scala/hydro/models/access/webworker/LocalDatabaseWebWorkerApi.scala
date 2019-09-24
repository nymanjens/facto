package hydro.models.access.webworker

import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js

trait LocalDatabaseWebWorkerApi {
  def create(dbName: String, inMemory: Boolean, separateDbPerCollection: Boolean): Future[Unit]

  def executeDataQuery(lokiQuery: LokiQuery): Future[Seq[js.Dictionary[js.Any]]]

  def executeCountQuery(lokiQuery: LokiQuery): Future[Int]

  /**
    * Executes the given operations in sequence.
    *
    * Returns true if database was modified (SaveDatabase doesn't count as modification).
    */
  def applyWriteOperations(operations: Seq[WriteOperation]): Future[Unit]
}
object LocalDatabaseWebWorkerApi {
  case class LokiQuery(
      collectionName: String,
      filter: Option[js.Dictionary[js.Any]] = None,
      sorting: Option[js.Array[js.Array[js.Any]]] = None,
      limit: Option[Int] = None,
  )

  sealed trait WriteOperation
  object WriteOperation {
    case class Insert(collectionName: String, obj: js.Dictionary[js.Any]) extends WriteOperation
    case class Update(collectionName: String, updatedObj: js.Dictionary[js.Any]) extends WriteOperation
    case class Remove(collectionName: String, id: js.Any) extends WriteOperation
    case class AddCollection(collectionName: String, uniqueIndices: Seq[String], indices: Seq[String])
        extends WriteOperation
    case class RemoveCollection(collectionName: String) extends WriteOperation
    case object SaveDatabase extends WriteOperation
  }

  object MethodNumbers {
    val create: Int = 1
    val executeDataQuery: Int = 2
    val executeCountQuery: Int = 3
    val applyWriteOperations: Int = 4
  }
}

package hydro.models.access.webworker

import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js

trait LocalDatabaseWebWorkerApi {

  /**
    * Creates a database with the given name and properties. If a database with that name already exists, this
    * method returns when that database is ready.
    */
  def createIfNecessary(dbName: String, inMemory: Boolean, separateDbPerCollection: Boolean): Future[Unit]

  def executeDataQuery(lokiQuery: LokiQuery): Future[Seq[js.Dictionary[js.Any]]]

  def executeCountQuery(lokiQuery: LokiQuery): Future[Int]

  /**
    * Executes the given operations in sequence.
    *
    * Returns true if database was modified (SaveDatabase doesn't count as modification).
    */
  def applyWriteOperations(operations: Seq[WriteOperation]): Future[Boolean]

  /**
    * Pure function (no side effects) that returns the operations that should be broadcasted.
    *
    * This should only be implemented by the worker server.
    */
  private[webworker] def getWriteOperationsToBroadcast(operations: Seq[WriteOperation]): Seq[WriteOperation]
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

    case class Update(
        collectionName: String,
        updatedObj: js.Dictionary[js.Any],
        abortUnlessExistingValueEquals: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
    ) extends WriteOperation

    case class Remove(collectionName: String, id: js.Any) extends WriteOperation

    case class AddCollection(
        collectionName: String,
        uniqueIndices: Seq[String],
        indices: Seq[String],
        // If true, BroadcastedWriteOperations will be sent for this collection
        broadcastUpdates: Boolean,
    ) extends WriteOperation

    case class RemoveCollection(collectionName: String) extends WriteOperation

    case object SaveDatabase extends WriteOperation
  }

  object MethodNumbers {
    val createIfNecessary: Int = 1
    val executeDataQuery: Int = 2
    val executeCountQuery: Int = 3
    val applyWriteOperations: Int = 4
  }

  sealed trait WorkerResponse
  object WorkerResponse {
    case class Failed(stackTrace: String) extends WorkerResponse
    case class MethodReturnValue(value: js.Any) extends WorkerResponse
    case class BroadcastedWriteOperations(operations: Seq[WriteOperation]) extends WorkerResponse
  }
}

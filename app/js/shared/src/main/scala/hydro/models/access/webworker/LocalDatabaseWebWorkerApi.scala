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
    * Returns true if database was modified.
    */
  def applyWriteOperations(operations: Seq[WriteOperation]): Future[Boolean]

  def saveDatabase(): Future[Unit]
}
object LocalDatabaseWebWorkerApi {

  private[webworker] trait ForServer extends LocalDatabaseWebWorkerApi {

    /** Pure function (no side effects) that returns the operations that should be broadcasted. */
    private[webworker] def getWriteOperationsToBroadcast(operations: Seq[WriteOperation]): Seq[WriteOperation]
  }

  trait ForClient extends LocalDatabaseWebWorkerApi {}

  case class LokiQuery(
      collectionName: String,
      filter: Option[js.Dictionary[js.Any]] = None,
      sorting: Option[js.Array[js.Array[js.Any]]] = None,
      limit: Option[Int] = None,
  )

  sealed trait WriteOperation {
    def collectionName: String
  }
  object WriteOperation {
    case class Insert(override val collectionName: String, obj: js.Dictionary[js.Any]) extends WriteOperation

    case class Update(
        override val collectionName: String,
        updatedObj: js.Dictionary[js.Any],
        abortUnlessExistingValueEquals: js.UndefOr[js.Dictionary[js.Any]] = js.undefined,
    ) extends WriteOperation

    case class Remove(override val collectionName: String, id: js.Any) extends WriteOperation

    case class AddCollection(
        override val collectionName: String,
        uniqueIndices: Seq[String],
        indices: Seq[String],
        // If true, BroadcastedWriteOperations will be sent for this collection
        broadcastWriteOperations: Boolean,
    ) extends WriteOperation

    case class RemoveCollection(override val collectionName: String) extends WriteOperation
  }

  object MethodNumbers {
    val createIfNecessary: Int = 1
    val executeDataQuery: Int = 2
    val executeCountQuery: Int = 3
    val applyWriteOperations: Int = 4
    val saveDatabase: Int = 5
  }

  sealed trait WorkerResponse
  object WorkerResponse {
    case class Failed(stackTrace: String) extends WorkerResponse
    case class MethodReturnValue(value: js.Any) extends WorkerResponse
    case class BroadcastedWriteOperations(operations: Seq[WriteOperation]) extends WorkerResponse
  }
}

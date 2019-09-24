package hydro.models.access.webworker

import hydro.jsfacades.LokiJs
import hydro.jsfacades.LokiJs.FilterFactory.Operation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[webworker] final class LocalDatabaseWebWorkerApiMultiDbImpl extends LocalDatabaseWebWorkerApi {

  private var collectionNameToDbMap: mutable.Map[String, LocalDatabaseWebWorkerApi] = mutable.Map()
  private var dbNamePrefix: String = _
  private var inMemory: Boolean = _
  private var changedCollectionsSinceLastSave: mutable.Set[String] = mutable.Set[String]()

  override def create(dbName: String, inMemory: Boolean, separateDbPerCollection: Boolean): Future[Unit] = {
    require(separateDbPerCollection)

    this.dbNamePrefix = dbName
    this.inMemory = inMemory
    Future.successful((): Unit)
  }

  override def executeDataQuery(
      lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Seq[js.Dictionary[js.Any]]] = async {
    val db = await(getDbForCollection(lokiQuery.collectionName))
    await(db.executeDataQuery(lokiQuery))
  }

  override def executeCountQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Int] = async {
    val db = await(getDbForCollection(lokiQuery.collectionName))
    await(db.executeCountQuery(lokiQuery))
  }

  override def applyWriteOperations(operations: Seq[WriteOperation]): Future[Unit] = {
    def performOperationOnCollection(operation: WriteOperation, collectionName: String): Future[Unit] =
      async {
        changedCollectionsSinceLastSave.add(collectionName)
        val db = await(getDbForCollection(collectionName))
        await(db.applyWriteOperations(Seq(operation)))
      }

    combineFuturesInOrder[WriteOperation](
      operations, {
        case operation @ Insert(collectionName, _) => performOperationOnCollection(operation, collectionName)
        case operation @ Update(collectionName, _) => performOperationOnCollection(operation, collectionName)
        case operation @ Remove(collectionName, _) => performOperationOnCollection(operation, collectionName)
        case operation @ AddCollection(collectionName, _, _) =>
          performOperationOnCollection(operation, collectionName)
        case operation @ RemoveCollection(collectionName) =>
          performOperationOnCollection(operation, collectionName)
        case operation @ SaveDatabase =>
          async {
            await(Future.sequence {
              changedCollectionsSinceLastSave.map { collectionName =>
                async {
                  val db = await(getDbForCollection(collectionName))
                  await(db.applyWriteOperations(Seq(operation)))
                }
              }
            })
            changedCollectionsSinceLastSave.clear()
          }
      }
    ).map(_ => (): Unit)
  }

  private def combineFuturesInOrder[T](
      iterable: Iterable[T],
      futureFunction: T => Future[Unit],
  ): Future[Unit] = {
    var result = Future.successful((): Unit)
    for (elem <- iterable) {
      result = result.flatMap(_ => futureFunction(elem))
    }
    result
  }

  private def getDbForCollection(collectionName: String): Future[LocalDatabaseWebWorkerApi] = async {
    collectionNameToDbMap.get(collectionName) match {
      case Some(db) => db
      case None =>
        val db = new LocalDatabaseWebWorkerApiImpl()
        await(
          db.create(
            dbName = s"${dbNamePrefix}_$collectionName",
            inMemory = inMemory,
            separateDbPerCollection = false))
        collectionNameToDbMap.put(collectionName, db)
        db
    }
  }
}

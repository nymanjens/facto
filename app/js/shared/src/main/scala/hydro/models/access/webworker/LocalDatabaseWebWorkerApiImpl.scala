package hydro.models.access.webworker

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import hydro.jsfacades.LokiJs
import hydro.jsfacades.LokiJs.FilterFactory.Operation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation._
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[webworker] final class LocalDatabaseWebWorkerApiImpl extends LocalDatabaseWebWorkerApi {
  private val nameToLokiDbs: mutable.Map[String, Future[LokiJs.Database]] = mutable.Map()
  private var currentLokiDb: LokiJs.Database = _

  override def createIfNecessary(dbName: String,
                                 inMemory: Boolean,
                                 separateDbPerCollection: Boolean): Future[Unit] = {
    require(!separateDbPerCollection)

    if (!nameToLokiDbs.contains(dbName)) {
      val newLokiDb =
        if (inMemory) {
          LokiJs.Database.inMemoryForTests(dbName)
        } else {
          LokiJs.Database.persistent(dbName)
        }

      nameToLokiDbs.put(dbName, async {
        await(newLokiDb.loadDatabase())
        newLokiDb
      })
    }

    nameToLokiDbs(dbName).map { db =>
      currentLokiDb = db
      (): Unit
    }
  }

  override def executeDataQuery(
      lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Seq[js.Dictionary[js.Any]]] =
    Future.successful(toResultSet(lokiQuery) match {
      case Some(r) => r.data().toVector
      case None    => Seq()
    })

  override def executeCountQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Int] =
    Future.successful(toResultSet(lokiQuery) match {
      case Some(r) => r.count()
      case None    => 0
    })

  private def toResultSet(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Option[LokiJs.ResultSet] = {
    currentLokiDb.getCollection(lokiQuery.collectionName) match {
      case None =>
        console.log(
          s"  Warning: Tried to query ${lokiQuery.collectionName}, but that collection doesn't exist")
        None

      case Some(lokiCollection) =>
        var resultSet = lokiCollection.chain()
        for (filter <- lokiQuery.filter) {
          resultSet = resultSet.find(filter)
        }
        for (sorting <- lokiQuery.sorting) {
          resultSet = resultSet.compoundsort(sorting)
        }
        for (limit <- lokiQuery.limit) {
          resultSet = resultSet.limit(limit)
        }
        Some(resultSet)
    }
  }

  override def applyWriteOperations(operations: Seq[WriteOperation]): Future[Boolean] = {
    Future
      .sequence(operations map {
        case Insert(collectionName, obj) =>
          Future.successful {
            val lokiCollection = getCollection(collectionName)
            findById(lokiCollection, obj("id")) match {
              case Some(entity) => false
              case None =>
                lokiCollection.insert(obj)
                true
            }
          }

        case Update(collectionName, updatedObj) =>
          Future.successful {
            val lokiCollection = getCollection(collectionName)
            findById(lokiCollection, updatedObj("id")) match {
              case None => false
              case Some(e) if e.filterKeys(k => k != "meta" && k != "$loki").toMap == updatedObj.toMap =>
                false
              case Some(e) =>
                lokiCollection.findAndRemove(
                  LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", updatedObj("id")))
                lokiCollection.insert(updatedObj)
                true
            }
          }

        case Remove(collectionName, id) =>
          Future.successful {
            val lokiCollection = getCollection(collectionName)
            findById(lokiCollection, id) match {
              case None => false
              case Some(entity) =>
                lokiCollection.findAndRemove(LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", id))
                true
            }
          }

        case AddCollection(collectionName, uniqueIndices, indices) =>
          Future.successful {
            if (currentLokiDb.getCollection(collectionName).isEmpty) {
              currentLokiDb.addCollection(
                collectionName,
                uniqueIndices = uniqueIndices,
                indices = indices
              )
              true
            } else {
              false
            }
          }

        case RemoveCollection(collectionName) =>
          Future.successful {
            currentLokiDb.removeCollection(collectionName)
            true
          }

        case SaveDatabase =>
          async {
            await(currentLokiDb.saveDatabase())
            false
          }
      })
      .map(changedSeq => changedSeq contains true)
  }

  private def findById(lokiCollection: LokiJs.Collection, id: js.Any): Option[js.Dictionary[js.Any]] = {
    lokiCollection
      .chain()
      .find(LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", id))
      .limit(1)
      .data()
      .toVector match {
      case Seq(e) => Some(e)
      case Seq()  => None
    }
  }

  private def getCollection(collectionName: String): LokiJs.Collection = {
    currentLokiDb
      .getCollection(collectionName)
      .getOrElse(throw new IllegalArgumentException(s"Could not get collection $collectionName"))
  }
}

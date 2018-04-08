package models.access.webworker

import jsfacades.LokiJs.FilterFactory.Operation
import jsfacades.{CryptoJs, LokiJs}
import models.Entity
import models.access.ModelField
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation._
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[webworker] final class LocalDatabaseWebWorkerApiImpl extends LocalDatabaseWebWorkerApi {
  private var lokiDb: LokiJs.Database = _

  override def create(dbName: String, encryptionSecret: String, inMemory: Boolean): Future[Unit] = {
    if (inMemory) {
      lokiDb = LokiJs.Database.inMemoryForTests(
        dbName,
        persistedStringCodex =
          if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
          else new LocalDatabaseWebWorkerApiImpl.EncryptingCodex(encryptionSecret)
      )
    } else {
      lokiDb = LokiJs.Database.persistent(
        dbName,
        persistedStringCodex =
          if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
          else new LocalDatabaseWebWorkerApiImpl.EncryptingCodex(encryptionSecret)
      )
    }

    lokiDb.loadDatabase()
  }

  override def executeDataQuery(
      lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Seq[js.Dictionary[js.Any]]] =
    Future.successful(toResultSet(lokiQuery).data().toVector)

  override def executeCountQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): Future[Int] =
    Future.successful(toResultSet(lokiQuery).count())

  private def toResultSet(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery): LokiJs.ResultSet = {
    val lokiCollection = getOrAddCollection(lokiQuery.collectionName)
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
    resultSet
  }

  override def applyWriteOperations(operations: Seq[WriteOperation]): Future[Boolean] = {
    Future
      .sequence(operations map {
        case Insert(collectionName, obj) =>
          val lokiCollection = getOrAddCollection(collectionName)
          findById(lokiCollection, obj("id")) match {
            case Some(entity) =>
              Future.successful(false)
            case None =>
              lokiCollection.insert(obj)
              Future.successful(true)
          }

        case Update(collectionName, updatedObj) =>
          val lokiCollection = getOrAddCollection(collectionName)
          findById(lokiCollection, updatedObj("id")) match {
            case None =>
              Future.successful(false)
            case Some(entity) =>
              lokiCollection.findAndRemove(
                LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", updatedObj("id")))
              lokiCollection.insert(updatedObj)
              Future.successful(true)
          }

        case Remove(collectionName, id) =>
          val lokiCollection = getOrAddCollection(collectionName)
          findById(lokiCollection, id) match {
            case None =>
              Future.successful(false)
            case Some(entity) =>
              lokiCollection.findAndRemove(LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", id))
              Future.successful(true)
          }

        case Clear(collectionName) =>
          val lokiCollection = lokiDb.getOrAddCollection(collectionName)
          lokiCollection.clear()
          Future.successful(true)

        case SaveDatabase =>
          lokiDb.saveDatabase().map(_ => false)
      })
      .map(_ contains true)
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

  private def getOrAddCollection(collectionName: String): LokiJs.Collection = {
    lokiDb.getCollection(collectionName) match {
      case Some(collection) => collection
      case None =>
        lokiDb.addCollection(
          collectionName,
          uniqueIndices = Seq("id")
        )
    }
  }
}
object LocalDatabaseWebWorkerApiImpl {

  private final class EncryptingCodex(secret: String) extends LokiJs.PersistedStringCodex {
    private val decodedPrefix = "DECODED"

    override def encodeBeforeSave(dbString: String) = {
      val millis1 = System.currentTimeMillis()
      console.log(s"  Encrypting ${dbString.length / 1e6}Mb String...")
      val result =
        CryptoJs.RC4Drop.encrypt(stringToEncrypt = decodedPrefix + dbString, password = secret).toString()
      val millis2 = System.currentTimeMillis()
      console.log(s"  Encrypting ${dbString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      result
    }

    override def decodeAfterLoad(encodedString: String) = {
      val millis1 = System.currentTimeMillis()
      console.log(s"  Decrypting ${encodedString.length / 1e6}Mb String...")
      val decoded =
        try {
          CryptoJs.RC4Drop
            .decrypt(stringToDecrypt = encodedString, password = secret)
            .toString(CryptoJs.Encoding.Utf8)
        } catch {
          case t: Throwable =>
            console.log(s"  Caught exception while decoding database string: $t")
            ""
        }
      val millis2 = System.currentTimeMillis()
      console.log(
        s"  Decrypting ${encodedString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      if (decoded.startsWith(decodedPrefix)) {
        Some(decoded.substring(decodedPrefix.length))
      } else {
        console.log(s"  Failed to decode database string: ${encodedString.substring(0, 10)}")
        None
      }
    }
  }
}

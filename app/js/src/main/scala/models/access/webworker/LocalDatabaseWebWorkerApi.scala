package models.access.webworker

import models.Entity
import models.access.DbQuery
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Scala2Js

trait LocalDatabaseWebWorkerApi {
  def create(dbName: String, encryptionSecret: String = "", inMemory: Boolean): Future[Unit]

  def applyWriteOperations(operations: WriteOperation*): Future[Boolean]
}
object LocalDatabaseWebWorkerApi {
  sealed trait WriteOperation
  object WriteOperation {
    case class Insert(collectionName: String, obj: js.Dictionary[js.Any]) extends WriteOperation
    case class FindAndRemove(collectionName: String, fieldName: String, fieldValue: js.Any)
        extends WriteOperation
    case class Clear(collectionName: String) extends WriteOperation
    case object SaveDatabase extends WriteOperation
  }
}

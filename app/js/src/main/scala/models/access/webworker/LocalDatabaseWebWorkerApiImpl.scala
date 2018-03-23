package models.access.webworker

import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation

import scala.concurrent.Future
import scala.scalajs.js

private[webworker] final class LocalDatabaseWebWorkerApiImpl extends LocalDatabaseWebWorkerApi {
  override def create(dbName: String, encryptionSecret: String, inMemory: Boolean) = ???
  override def applyWriteOperations(operations: WriteOperation*) = ???
}

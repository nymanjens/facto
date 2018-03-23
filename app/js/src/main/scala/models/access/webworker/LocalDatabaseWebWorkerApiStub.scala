package models.access.webworker

import api.Picklers._
import common.LoggingUtils.logExceptions
import models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import org.scalajs.dom
import org.scalajs.dom.raw.Worker

import scala.async.Async.{async, await}
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala2js.Converters._
import scala2js.Scala2Js

final class LocalDatabaseWebWorkerApiStub extends LocalDatabaseWebWorkerApi {

  private val responseMessagePromises: mutable.Buffer[Promise[js.Any]] = mutable.Buffer()
  private val worker: Worker = initializeWebWorker()

  override def create(dbName: String, encryptionSecret: String, inMemory: Boolean) = {
    sendAndReceive(MethodNumbers.create, dbName, encryptionSecret, inMemory).map(_ => (): Unit)
  }
  override def applyWriteOperations(operations: LocalDatabaseWebWorkerApi.WriteOperation*) =
    sendAndReceive(MethodNumbers.applyWriteOperations, Scala2Js.toJs(operations.toList))
      .map(_.asInstanceOf[Boolean])

  private def sendAndReceive(methodNum: Int, args: js.Any*): Future[js.Any] = async {
    val lastMessagePromise: Option[Promise[_]] = responseMessagePromises.lastOption
    val thisMessagePromise: Promise[js.Any] = Promise()
    responseMessagePromises += thisMessagePromise

    if (lastMessagePromise.isDefined) {
      await(lastMessagePromise.get.future)
    }

    logExceptions {
      worker.postMessage(js.Array(methodNum, args.toJSArray))
    }

    await(thisMessagePromise.future)
  }

  private def initializeWebWorker(): Worker = {
    val worker = new Worker("/localDatabaseWebWorker.js")
    worker.onmessage = (e: js.Any) => {
      val data = e.asInstanceOf[dom.MessageEvent].data.asInstanceOf[js.Any]

      responseMessagePromises.headOption match {
        case Some(promise) if promise.isCompleted =>
          throw new AssertionError("First promise in responseMessagePromises is completed. This is a bug!")
        case Some(promise) =>
          responseMessagePromises.remove(0)
          promise.success(data)
        case None =>
          throw new AssertionError(s"Received unexpected message: $data")
      }
    }
    worker
  }
}

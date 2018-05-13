package models.access.webworker

import api.Picklers._
import common.LoggingUtils.logExceptions
import models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import org.scalajs.dom
import org.scalajs.dom.raw.Worker

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.scalajs.js.JSConverters._
import scala2js.Converters._
import scala2js.Scala2Js

private[webworker] final class LocalDatabaseWebWorkerApiStub extends LocalDatabaseWebWorkerApi {

  private val responseMessagePromises: mutable.Buffer[Promise[js.Any]] = mutable.Buffer()
  private val worker: Worker = initializeWebWorker()

  override def create(dbName: String, encryptionSecret: String, inMemory: Boolean) = {
    sendAndReceive(MethodNumbers.create, dbName, encryptionSecret, inMemory).map(_ => (): Unit)
  }

  override def executeDataQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery) =
    sendAndReceive(MethodNumbers.executeDataQuery, Scala2Js.toJs(lokiQuery))
      .map(_.asInstanceOf[js.Array[js.Dictionary[js.Any]]].toVector)

  override def executeCountQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery) =
    sendAndReceive(MethodNumbers.executeCountQuery, Scala2Js.toJs(lokiQuery)).map(_.asInstanceOf[Int])

  override def applyWriteOperations(operations: Seq[LocalDatabaseWebWorkerApi.WriteOperation]) =
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
          if (data == Scala2Js.toJs("FAILED")) {
            promise.failure(new IllegalStateException("WebWorker invocation failed"))
          } else {
            promise.success(data)
          }
        case None =>
          throw new AssertionError(s"Received unexpected message: $data")
      }
    }
    worker
  }
}
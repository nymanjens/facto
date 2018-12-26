package app.models.access.webworker

import app.api.Picklers._
import hydro.common.LoggingUtils.logExceptions
import app.models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import app.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import org.scalajs
import org.scalajs.dom
import org.scalajs.dom.raw.Worker

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import app.scala2js.Converters._
import hydro.scala2js.Scala2Js

private[webworker] final class LocalDatabaseWebWorkerApiStub extends LocalDatabaseWebWorkerApi {

  private val responseMessagePromises: mutable.Buffer[Promise[js.Any]] = mutable.Buffer()
  private val worker: Worker = initializeWebWorker()

  override def create(dbName: String, inMemory: Boolean) = {
    sendAndReceive(MethodNumbers.create, Seq(dbName, inMemory), timeout = 10.seconds).map(_ => (): Unit)
  }

  override def executeDataQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery) =
    sendAndReceive(MethodNumbers.executeDataQuery, Seq(Scala2Js.toJs(lokiQuery)))
      .map(_.asInstanceOf[js.Array[js.Dictionary[js.Any]]].toVector)

  override def executeCountQuery(lokiQuery: LocalDatabaseWebWorkerApi.LokiQuery) =
    sendAndReceive(MethodNumbers.executeCountQuery, Seq(Scala2Js.toJs(lokiQuery))).map(_.asInstanceOf[Int])

  override def applyWriteOperations(operations: Seq[LocalDatabaseWebWorkerApi.WriteOperation]) =
    sendAndReceive(
      MethodNumbers.applyWriteOperations,
      Seq(Scala2Js.toJs(operations.toList)),
      timeout = 1.minute)
      .map(_ => (): Unit)

  private def sendAndReceive(methodNum: Int,
                             args: Seq[js.Any],
                             timeout: FiniteDuration = 5.seconds): Future[js.Any] = async {
    val lastMessagePromise: Option[Promise[_]] = responseMessagePromises.lastOption
    val thisMessagePromise: Promise[js.Any] = Promise()
    responseMessagePromises += thisMessagePromise

    if (lastMessagePromise.isDefined) {
      await(lastMessagePromise.get.future)
    }

    logExceptions {
      worker.postMessage(js.Array(methodNum, args.toJSArray))
    }

    js.timers.setTimeout(timeout) {
      if (!thisMessagePromise.isCompleted) {
        scalajs.dom.console
          .log(
            "  [LocalDatabaseWebWorker] Operation timed out " +
              s"(methodNum = $methodNum, args = $args, timeout = $timeout)")
      }
      thisMessagePromise.tryFailure(
        new Exception(s"Operation timed out (methodNum = $methodNum, args = $args, timeout = $timeout)"))
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

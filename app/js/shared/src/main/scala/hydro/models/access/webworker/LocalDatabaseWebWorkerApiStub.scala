package hydro.models.access.webworker

import hydro.common.JsLoggingUtils.logExceptions
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WorkerResponse
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import hydro.models.access.worker.JsWorkerClientFacade
import hydro.models.access.worker.JsWorkerClientFacade.JsWorkerClient
import hydro.scala2js.Scala2Js
import org.scalajs

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

final class LocalDatabaseWebWorkerApiStub(
    forceJsWorker: Option[JsWorkerClientFacade] = None
) extends LocalDatabaseWebWorkerApi.ForClient {

  private val responseMessagePromises: mutable.Buffer[Promise[js.Any]] = mutable.Buffer()
  private val worker: JsWorkerClient = initializeJsWorker()
  private var listeners: Seq[LocalDatabaseWebWorkerApi.ForClient.Listener] = Seq()

  override def createIfNecessary(dbName: String, inMemory: Boolean, separateDbPerCollection: Boolean) = {
    sendAndReceive(
      MethodNumbers.createIfNecessary,
      Seq(dbName, inMemory, separateDbPerCollection),
      timeout = 40.seconds,
    ).map(_ => (): Unit)
  }

  override def executeDataQuery(lokiQuery: LokiQuery) =
    sendAndReceive(
      MethodNumbers.executeDataQuery,
      Seq(Scala2Js.toJs(lokiQuery)),
      timeout = 40.seconds,
    ).map(_.asInstanceOf[js.Array[js.Dictionary[js.Any]]].toVector)

  override def executeCountQuery(lokiQuery: LokiQuery) =
    sendAndReceive(
      MethodNumbers.executeCountQuery,
      Seq(Scala2Js.toJs(lokiQuery)),
      timeout = 40.seconds,
    ).map(_.asInstanceOf[Int])

  override def applyWriteOperations(operations: Seq[WriteOperation]) =
    sendAndReceive(
      MethodNumbers.applyWriteOperations,
      Seq(Scala2Js.toJs(operations.toList)),
      timeout = 2.minutes,
    ).map(_.asInstanceOf[Boolean])

  override def saveDatabase() =
    sendAndReceive(
      MethodNumbers.saveDatabase,
      Seq(),
      timeout = 2.minutes,
    ).map(_ => (): Unit)

  override def registerListener(listener: LocalDatabaseWebWorkerApi.ForClient.Listener): Unit = {
    listeners = listeners :+ listener
  }

  private def sendAndReceive(methodNum: Int, args: Seq[js.Any], timeout: FiniteDuration): Future[js.Any] =
    async {
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
                s"(methodNum = $methodNum, args = $args, timeout = $timeout)"
            )
          responseMessagePromises -= thisMessagePromise
          thisMessagePromise.failure(
            new Exception(s"Operation timed out (methodNum = $methodNum, args = $args, timeout = $timeout)")
          )
        }
      }

      await(thisMessagePromise.future)
    }

  private def initializeJsWorker(): JsWorkerClient = {
    val workerClientFacade =
      forceJsWorker orElse
        JsWorkerClientFacade.getSharedIfSupported() getOrElse
        JsWorkerClientFacade.getDedicated()

    workerClientFacade.setUpClient(
      scriptUrl = "/localDatabaseWebWorker.js",
      onMessage = data =>
        logExceptions {
          Scala2Js.toScala[WorkerResponse](data) match {
            case response @ WorkerResponse.Failed(stackTrace) =>
              responseMessagePromises.headOption match {
                case Some(promise) =>
                  responseMessagePromises.remove(0)
                  promise.failure(new IllegalStateException(s"WebWorker invocation failed:\n$stackTrace"))
                case None =>
                  throw new AssertionError(
                    s"Received unexpected message (this is a bug unless this operation timed out): $response"
                  )
              }

            case response @ WorkerResponse.MethodReturnValue(returnValue) =>
              responseMessagePromises.headOption match {
                case Some(promise) =>
                  responseMessagePromises.remove(0)
                  promise.success(returnValue)
                case None =>
                  throw new AssertionError(
                    s"Received unexpected message (this is a bug unless this operation timed out): $response"
                  )
              }

            case WorkerResponse.BroadcastedWriteOperations(writeOperations) =>
              for (listener <- listeners) {
                listener.onWriteOperationsDone(writeOperations)
              }
          }
        },
    )
  }
}

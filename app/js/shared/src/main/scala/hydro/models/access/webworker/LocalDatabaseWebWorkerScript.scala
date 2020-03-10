package hydro.models.access.webworker

import java.io.PrintWriter
import java.io.StringWriter

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import hydro.common.JsLoggingUtils.logExceptions
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WorkerResponse
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import hydro.models.access.worker.JsWorkerServerFacade
import hydro.models.access.worker.JsWorkerServerFacade.OnMessageResponse
import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters._
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object LocalDatabaseWebWorkerScript {

  private val separateDbPerCollectionToApiImplMap: mutable.Map[Boolean, LocalDatabaseWebWorkerApi.ForServer] =
    mutable.Map()
  private var currentApiImpl: LocalDatabaseWebWorkerApi.ForServer = _

  def run(): Unit = {
    JsWorkerServerFacade
      .getFromGlobalScope()
      .setUpFromWorkerScript(new WorkerScriptLogic {
        override def onMessage(data: js.Any): Future[OnMessageResponse] = {
          // Flatmap dummy future so that exceptions being thrown by method invocation and in returned future
          // get treated the same
          Future.successful((): Unit).flatMap { _ =>
            logExceptions {
              data.asInstanceOf[js.Array[_]].toVector match {
                case Seq(methodNum, args) =>
                  executeMethod(methodNum.asInstanceOf[Int], args.asInstanceOf[js.Array[js.Any]])
              }
            }
          } recover {
            case e: Throwable =>
              console.log(s"  LocalDatabaseWebWorkerScript: Caught exception: $e")
              e.printStackTrace()
              OnMessageResponse(
                response = Scala2Js.toJs[WorkerResponse](WorkerResponse.Failed(getStackTraceString(e))))
          }
        }
      })
  }

  private def executeMethod(methodNum: Int, args: js.Array[js.Any]): Future[OnMessageResponse] = {
    def toResponse(returnValue: js.Any): OnMessageResponse = {
      OnMessageResponse(
        response = Scala2Js.toJs[WorkerResponse](WorkerResponse.MethodReturnValue(returnValue)),
      )
    }

    (methodNum, args.toVector) match {
      case (MethodNumbers.createIfNecessary, Seq(dbName, inMemory, separateDbPerCollectionObj)) =>
        val separateDbPerCollection = separateDbPerCollectionObj.asInstanceOf[Boolean]
        if (!separateDbPerCollectionToApiImplMap.contains(separateDbPerCollection)) {
          separateDbPerCollectionToApiImplMap.update(
            separateDbPerCollection,
            if (separateDbPerCollection) new LocalDatabaseWebWorkerApiMultiDbImpl()
            else new LocalDatabaseWebWorkerApiImpl())
        }
        currentApiImpl = separateDbPerCollectionToApiImplMap(separateDbPerCollection)
        currentApiImpl
          .createIfNecessary(
            dbName.asInstanceOf[String],
            inMemory.asInstanceOf[Boolean],
            separateDbPerCollection)
          .map(_ => toResponse(js.undefined))
      case (MethodNumbers.executeDataQuery, Seq(lokiQuery)) =>
        currentApiImpl
          .executeDataQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => toResponse(r.toJSArray))
      case (MethodNumbers.executeCountQuery, Seq(lokiQuery)) =>
        currentApiImpl
          .executeCountQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => toResponse(r))
      case (MethodNumbers.applyWriteOperations, Seq(operationsJsValue)) =>
        async {
          val operations = Scala2Js.toScala[Seq[WriteOperation]](operationsJsValue)
          val returnValue = await(currentApiImpl.applyWriteOperations(operations))
          val operationsToBroadcast = currentApiImpl.getWriteOperationsToBroadcast(operations)

          OnMessageResponse(
            response = Scala2Js.toJs[WorkerResponse](WorkerResponse.MethodReturnValue(returnValue)),
            responseToBroadcastToOtherPorts =
              Scala2Js.toJs[WorkerResponse](WorkerResponse.BroadcastedWriteOperations(operationsToBroadcast)),
          )
        }
      case (MethodNumbers.saveDatabase, Seq()) =>
        currentApiImpl.saveDatabase().map(_ => toResponse(js.undefined))
    }
  }

  private def getStackTraceString(throwable: Throwable): String = {
    val sw = new StringWriter
    throwable.printStackTrace(new PrintWriter(sw))
    sw.toString
  }
}

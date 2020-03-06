package hydro.models.access.webworker

import hydro.common.JsLoggingUtils.logExceptions
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import hydro.models.access.worker.JsWorkerServerFacade
import hydro.models.access.worker.JsWorkerServerFacade.OnMessageResponse
import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters._
import org.scalajs.dom
import org.scalajs.dom.console
import org.scalajs.dom.MessagePort
import org.scalajs.dom.experimental.serviceworkers.ExtendableMessageEvent
import org.scalajs.dom.experimental.sharedworkers.SharedWorkerGlobalScope
import org.scalajs.dom.raw.MessageEvent

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.Failure
import scala.util.Success

object LocalDatabaseWebWorkerScript {

  private val separateDbPerCollectionToApiImplMap: mutable.Map[Boolean, LocalDatabaseWebWorkerApi] =
    mutable.Map()
  private var currentApiImpl: LocalDatabaseWebWorkerApi = _

  def run(): Unit = {
    JsWorkerServerFacade
      .getAllSupported()
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
          } map { result =>
            OnMessageResponse(response = result)
          } recover {
            case e: Throwable =>
              console.log(s"  LocalDatabaseWebWorkerScript: Caught exception: $e")
              e.printStackTrace()
              OnMessageResponse(response = "FAILED") // signal to caller that call failed
          }
        }
      })
  }

  private def executeMethod(methodNum: Int, args: js.Array[js.Any]): Future[js.Any] = {
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
          .map(_ => js.undefined)
      case (MethodNumbers.executeDataQuery, Seq(lokiQuery)) =>
        currentApiImpl
          .executeDataQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => r.toJSArray)
      case (MethodNumbers.executeCountQuery, Seq(lokiQuery)) =>
        currentApiImpl
          .executeCountQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => r)
      case (MethodNumbers.applyWriteOperations, Seq(operations)) =>
        currentApiImpl
          .applyWriteOperations(Scala2Js.toScala[Seq[WriteOperation]](operations))
          .map(r => r)
    }
  }
}

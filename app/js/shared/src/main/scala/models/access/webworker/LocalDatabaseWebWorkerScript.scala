package models.access.webworker

import api.Picklers._
import jsfacades.WebWorker
import models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import org.scalajs.dom
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.Failure
import scala.util.Success
import scala2js.Converters._
import scala2js.Scala2Js

object LocalDatabaseWebWorkerScript {

  private val apiImpl = new LocalDatabaseWebWorkerApiImpl()

  def run(): Unit = {
    WebWorker.addEventListener("message", onMessage _)
  }

  private def onMessage(msg: dom.MessageEvent) = {
    val data = msg.data.asInstanceOf[js.Array[js.Any]].toVector

    // Flatmap dummy future so that exceptions being thrown my method invocation and in returned future
    // get treated the same
    Future.successful((): Unit).flatMap { _ =>
      data match {
        case Seq(methodNum, args) =>
          executeMethod(methodNum.asInstanceOf[Int], args.asInstanceOf[js.Array[js.Any]])
      }
    } onComplete {
      case Success(result) =>
        WebWorker.postMessage(result)
      case Failure(e) =>
        console.log(s"  LocalDatabaseWebWorkerScript: Caught exception: $e")
        e.printStackTrace()
        WebWorker.postMessage("FAILED") // signal to caller that call failed
    }
  }

  private def executeMethod(methodNum: Int, args: js.Array[js.Any]): Future[js.Any] = {
    (methodNum, args.toVector) match {
      case (MethodNumbers.create, Seq(dbName, inMemory)) =>
        apiImpl
          .create(dbName.asInstanceOf[String], inMemory.asInstanceOf[Boolean])
          .map(_ => js.undefined)
      case (MethodNumbers.executeDataQuery, Seq(lokiQuery)) =>
        apiImpl
          .executeDataQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => r.toJSArray)
      case (MethodNumbers.executeCountQuery, Seq(lokiQuery)) =>
        apiImpl
          .executeCountQuery(Scala2Js.toScala[LokiQuery](lokiQuery))
          .map(r => r)
      case (MethodNumbers.applyWriteOperations, Seq(operations)) =>
        apiImpl
          .applyWriteOperations(Scala2Js.toScala[Seq[WriteOperation]](operations))
          .map(r => r)
    }
  }
}

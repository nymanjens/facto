package hydro.models.access.entityaccessworker

import app.api.ScalaJsApi.GetInitialDataResponse
import hydro.common.GuavaReplacement.Stopwatch
import hydro.common.JsLoggingUtils.logExceptions
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.MethodNumbers
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters._
import org.scalajs
import org.scalajs
import org.scalajs.dom
import org.scalajs.dom.experimental.sharedworkers.SharedWorker
import org.scalajs.dom.raw.Worker

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
import scala.scalajs.js.timers
import scala.util.Random

final class EntityAccessSharedWorkerClient {

  private val randomNumber: String = Math.abs(Random.nextInt()).toString.substring(0, 2)
  private val worker: SharedWorker = initializeWebWorker()
  private var stopwatch: Stopwatch = Stopwatch.createStarted()

  private def initializeWebWorker(): SharedWorker = logExceptions {
    println(s"!! [$randomNumber] Starting shared worker")
    val worker = new SharedWorker("/entityAccessSharedWorker.js")
    worker.port.onmessage = (e: js.Any) => {
      val data = e.asInstanceOf[dom.MessageEvent].data.asInstanceOf[js.Any]

      scalajs.dom.console
        .log(s"!! [$randomNumber, ${stopwatch.elapsed.toNanos / 1000000.0}]  RECEIVED MESSAGE:", data)
      println(data.asInstanceOf[GetInitialDataResponse])

    }
    worker.port.start()
    println(s"!! [$randomNumber] Starting shared worker: Done")
    worker
  }

  worker.port.postMessage(s"[$randomNumber] Test message A")
  timers.setInterval(10.seconds) {
    stopwatch = Stopwatch.createStarted()
    worker.port.postMessage(s"[$randomNumber] Test message B")
  }
}

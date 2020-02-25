package hydro.models.access.entityaccessworker

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import org.scalajs.dom.experimental.sharedworkers.SharedWorkerGlobalScope
import org.scalajs.dom.MessagePort
import org.scalajs.dom.experimental.serviceworkers.ExtendableMessageEvent
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope
import org.scalajs.dom.raw.MessageEvent

import scala.collection.mutable
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers
import scala.util.Random

object EntityAccessSharedWorkerScript {

  val randomNumber: String = Math.abs(Random.nextInt()).toString.substring(0, 2)
  val connectedPorts: mutable.Buffer[MessagePort] = mutable.Buffer()

  def run(): Unit = {
    SharedWorkerGlobalScope.self.addEventListener(
      "connect",
      (connectEvent: ExtendableMessageEvent) => {
        val port: MessagePort = connectEvent.ports(0)
        connectedPorts.append(port)
        port.start()

        port.addEventListener(
          "message",
          (messageEvent: MessageEvent) => {
            println(s" >>> [$randomNumber] got message: ${messageEvent.data}")
            port.postMessage(s"[$randomNumber] reply to ${messageEvent.data}")
          }
        )
      }
    )

    //startBroadcastingEveryXSeconds()
    callApi()
  }

  private def startBroadcastingEveryXSeconds(): Unit = {
    timers.setInterval(9.seconds) {
      println(s" >>> [$randomNumber] #ports = ${connectedPorts.size}")
      for (port <- connectedPorts) {
        port.postMessage(s"[$randomNumber] Broadcast")
      }
    }
  }

  private def callApi(): Unit = async {
    val commonTimeModule = new hydro.common.time.Module
    implicit val clock = commonTimeModule.clock

    val apiModule = new app.api.Module
    implicit val scalaJsApiClient = apiModule.scalaJsApiClient

    val iniitalData = await(scalaJsApiClient.getInitialData())
    println(s"!!!!!!!!!!!!!>>>>>>>>>> initialData = ${iniitalData}")

    timers.setInterval(4.seconds) {
      for (port <- connectedPorts) {
        port.postMessage(iniitalData.asInstanceOf[js.Any])
      }
    }
  }
}

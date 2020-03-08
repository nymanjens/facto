package hydro.models.access.worker.impl

import hydro.models.access.worker.JsWorkerClientFacade
import hydro.models.access.worker.JsWorkerClientFacade.JsWorkerClient
import hydro.models.access.worker.JsWorkerServerFacade
import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import org.scalajs.dom
import org.scalajs.dom.MessagePort
import org.scalajs.dom.experimental.serviceworkers.ExtendableMessageEvent
import org.scalajs.dom.experimental.sharedworkers.SharedWorker
import org.scalajs.dom.experimental.sharedworkers.SharedWorkerGlobalScope

import scala.collection.mutable
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[worker] object SharedWorkerFacadeImpl extends JsWorkerClientFacade with JsWorkerServerFacade {

  override def setUpClient(scriptUrl: String, onMessage: js.Any => Unit): JsWorkerClient = {
    println("  Setting up SharedWorker client...")

    val worker = new SharedWorker(scriptUrl)
    worker.port.onmessage = (event: dom.MessageEvent) => {
      onMessage(event.data.asInstanceOf[js.Any])
    }
    worker.port.start()

    println("  Setting up SharedWorker client: Done")

    new JsWorkerClient {
      override def postMessage(message: js.Any): Unit = worker.port.postMessage(message)
    }
  }

  override def setUpFromWorkerScript(workerScriptLogic: WorkerScriptLogic): Unit = {
    println("  Setting up SharedWorker server...")

    val connectedPorts: mutable.Buffer[MessagePort] = mutable.Buffer()

    def onMessage(senderPort: MessagePort)(msg: dom.MessageEvent) = {
      workerScriptLogic.onMessage(msg.data.asInstanceOf[js.Any]) foreach { response =>
        senderPort.postMessage(message = response.response)

        if (!js.isUndefined(response.responseToBroadcastToOtherPorts)) {
          for {
            port <- connectedPorts
            if port != senderPort
          } port.postMessage(response.responseToBroadcastToOtherPorts)
        }
      }
    }

    SharedWorkerGlobalScope.self.addEventListener(
      "connect",
      (connectEvent: ExtendableMessageEvent) => {
        val port: MessagePort = connectEvent.ports(0)
        connectedPorts.append(port)
        port.addEventListener("message", onMessage(senderPort = port))
        port.start()
      }
    )

    println("  Setting up SharedWorker server: Done")
  }

}

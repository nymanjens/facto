package hydro.models.access.worker.impl

import hydro.models.access.worker.JsWorkerClientFacade
import hydro.models.access.worker.JsWorkerClientFacade.JsWorkerClient
import hydro.models.access.worker.JsWorkerServerFacade
import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import org.scalajs.dom
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope
import org.scalajs.dom.webworkers.Worker

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[worker] object DedicatedWorkerFacadeImpl extends JsWorkerClientFacade with JsWorkerServerFacade {

  override def setUpClient(scriptUrl: String, onMessage: js.Any => Unit): JsWorkerClient = {
    println("  Setting up DedicatedWorker client...")

    val worker = new Worker(scriptUrl)
    worker.onmessage = (event: dom.MessageEvent) => {
      onMessage(event.data.asInstanceOf[js.Any])
    }

    println("  Setting up DedicatedWorker client: Done")

    new JsWorkerClient {
      override def postMessage(message: js.Any): Unit = worker.postMessage(message)
    }
  }

  override def setUpFromWorkerScript(workerScriptLogic: WorkerScriptLogic): Unit = {
    println("  Setting up DedicatedWorker server...")

    def onMessage(msg: dom.MessageEvent) = {
      workerScriptLogic.onMessage(msg.data.asInstanceOf[js.Any]) foreach { response =>
        DedicatedWorkerGlobalScope.self.postMessage(response.response)
      }
    }

    DedicatedWorkerGlobalScope.self.addEventListener("message", onMessage _)

    println("  Setting up DedicatedWorker server: Done")
  }

}

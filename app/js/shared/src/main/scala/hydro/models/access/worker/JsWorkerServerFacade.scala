package hydro.models.access.worker

import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import hydro.models.access.worker.impl.SharedWorkerFacadeImpl
import org.scalajs.dom.experimental.sharedworkers.SharedWorkerGlobalScope

import scala.concurrent.Future
import scala.scalajs.js

trait JsWorkerServerFacade {
  def setUpFromWorkerScript(workerScriptLogic: WorkerScriptLogic): Unit
}
object JsWorkerServerFacade {

  def getAllSupported(): JsWorkerServerFacade = new JsWorkerServerFacade {
    override def setUpFromWorkerScript(workerScriptLogic: WorkerScriptLogic): Unit = {
      if (!js.isUndefined(SharedWorkerGlobalScope.self.onconnect)) {
        SharedWorkerFacadeImpl.setUpFromWorkerScript(workerScriptLogic)
      }
    }
  }

  trait WorkerScriptLogic {
    def onMessage(data: js.Any): Future[OnMessageResponse]

  }

  case class OnMessageResponse(response: js.Any, responseToBroadcastToOtherPorts: js.Any = js.undefined)
}

package hydro.models.access.worker

import hydro.models.access.worker.JsWorkerServerFacade.WorkerScriptLogic
import hydro.models.access.worker.impl.DedicatedWorkerFacadeImpl
import hydro.models.access.worker.impl.SharedWorkerFacadeImpl
import org.scalajs.dom.experimental.sharedworkers.SharedWorkerGlobalScope
import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope

import scala.concurrent.Future
import scala.scalajs.js

trait JsWorkerServerFacade {
  def setUpFromWorkerScript(workerScriptLogic: WorkerScriptLogic): Unit
}
object JsWorkerServerFacade {

  def getFromGlobalScope(): JsWorkerServerFacade = {
    if (!js.isUndefined(js.Dynamic.global.onconnect)) {
      SharedWorkerFacadeImpl
    } else if (!js.isUndefined(js.Dynamic.global.onmessage)) {
      DedicatedWorkerFacadeImpl
    } else {
      throw new AssertionError("This global scope supports none of the implemented workers")
    }
  }

  trait WorkerScriptLogic {
    def onMessage(data: js.Any): Future[OnMessageResponse]

  }

  case class OnMessageResponse(response: js.Any, responseToBroadcastToOtherPorts: js.Any = js.undefined)
}

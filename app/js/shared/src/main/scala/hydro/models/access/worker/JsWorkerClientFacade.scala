package hydro.models.access.worker

import hydro.common.ScalaUtils.ifThenOption
import hydro.models.access.worker.JsWorkerClientFacade.JsWorkerClient
import hydro.models.access.worker.impl.DedicatedWorkerFacadeImpl
import hydro.models.access.worker.impl.SharedWorkerFacadeImpl

import scala.scalajs.js

trait JsWorkerClientFacade {
  def setUpClient(scriptUrl: String, onMessage: js.Any => Unit): JsWorkerClient
}
object JsWorkerClientFacade {

  def getSharedIfSupported(): Option[JsWorkerClientFacade] = {
    ifThenOption(!js.isUndefined(js.Dynamic.global.SharedWorker)) {
      SharedWorkerFacadeImpl
    }
  }
  def getDedicated(): JsWorkerClientFacade = DedicatedWorkerFacadeImpl

  trait JsWorkerClient {
    def postMessage(message: js.Any): Unit
  }
}

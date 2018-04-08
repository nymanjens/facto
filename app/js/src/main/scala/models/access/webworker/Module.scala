package models.access.webworker

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import common.time.Clock
import models.user.User

import scala.concurrent.Future

final class Module() {

  val localDatabaseWebWorkerApiStub: LocalDatabaseWebWorkerApi = new LocalDatabaseWebWorkerApiStub()
}

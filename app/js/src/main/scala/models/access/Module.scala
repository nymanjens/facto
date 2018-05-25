package models.access

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import models.user.User

final class Module(implicit user: User,
                   scalaJsApiClient: ScalaJsApiClient,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit val entityAccess: JsEntityAccess = {
    val webWorkerModule = new models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create(encryptionSecret = user.databaseEncryptionKey)
    implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabaseFuture)
    val entityAccess = new JsEntityAccessImpl(getInitialDataResponse.allUsers)

    entityAccess.startSchedulingModifiedEntityUpdates()

    entityAccess
  }
}

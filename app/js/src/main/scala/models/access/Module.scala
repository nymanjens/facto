package models.access

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import models.user.User

final class Module(implicit user: User,
                   scalaJsApiClient: ScalaJsApiClient,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit private val entityModificationPushClientFactory: EntityModificationPushClientFactory =
    new EntityModificationPushClientFactory()

  implicit val entityAccess: JsEntityAccess = {
    val webWorkerModule = new models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create()
    implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabaseFuture)
    val entityAccess = new JsEntityAccessImpl(getInitialDataResponse.allUsers)

    entityAccess.startCheckingForModifiedEntityUpdates()

    entityAccess
  }
}

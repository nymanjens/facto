package app.models.access

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.user.User

final class Module(implicit user: User,
                   scalaJsApiClient: ScalaJsApiClient,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit val entityModificationPushClientFactory: EntityModificationPushClientFactory =
    new EntityModificationPushClientFactory()

  implicit val entityAccess: AppJsEntityAccess = {
    val webWorkerModule = new hydro.models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create()
    implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabaseFuture)
    val entityAccess = new AppJsEntityAccess(getInitialDataResponse.allUsers)

    entityAccess.startCheckingForModifiedEntityUpdates()

    entityAccess
  }
}

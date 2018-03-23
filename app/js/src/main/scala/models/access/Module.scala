package models.access

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import common.time.Clock
import models.user.User

import scala.concurrent.Future

final class Module(user: User)(implicit scalaJsApiClient: ScalaJsApiClient,
                               clock: Clock,
                               getInitialDataResponse: GetInitialDataResponse) {

  private val webworkerModule = new models.access.webworker.Module()
  implicit private val localDatabaseWebWorkerApiStub = webworkerModule.localDatabaseWebWorkerApiStub

  // Use LocallyClonedJsEntityAccess
//  implicit val entityAccess: Future[JsEntityAccess] = async {
//    val db = await(LocalDatabase.createFuture(encryptionSecret = user.databaseEncryptionKey))
//    val entityAccess =
//      await(LocallyClonedJsEntityAccess.create(scalaJsApiClient, db, getInitialDataResponse.allUsers))
//
//    entityAccess.startSchedulingModifiedEntityUpdates()
//
//    entityAccess
//  }

  // Use ApiBackedJsEntityAccess
  implicit val entityAccess: Future[JsEntityAccess] = {
    implicit val remoteDatabaseProxy = new ApiBackedRemoteDatabaseProxy()
    val entityAccess = new JsEntityAccessImpl(getInitialDataResponse.allUsers)

    entityAccess.startSchedulingModifiedEntityUpdates()

    Future.successful(entityAccess)
  }
}

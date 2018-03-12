package models.access

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import common.time.Clock
import models.user.User

import scala.concurrent.Future

final class Module(user: User)(implicit scalaJsApiClient: ScalaJsApiClient,
                               clock: Clock,
                               getInitialDataResponse: GetInitialDataResponse) {

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
    val entityAccess = new ApiBackedJsEntityAccess(getInitialDataResponse.allUsers)

    entityAccess.startSchedulingModifiedEntityUpdates()

    Future.successful(entityAccess)
  }
}

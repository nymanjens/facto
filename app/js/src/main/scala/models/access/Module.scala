package models.access

import api.ScalaJsApiClient
import common.time.Clock
import models.user.User

import scala.concurrent.Future

final class Module(user: User)(implicit scalaJsApiClient: ScalaJsApiClient, clock: Clock) {

//  private lazy val localDatabase: Future[LocalDatabase] =
//    LocalDatabase.createFuture(encryptionSecret = user.databaseEncryptionKey)
//  implicit val remoteDatabaseProxy: Future[RemoteDatabaseProxy] = async {
//    val db = await(localDatabase)
//    val proxy = await(LocallyClonedRemoteDatabaseProxy.create(scalaJsApiClient, db))
//
//    proxy.startSchedulingModifiedEntityUpdates()
//
//    proxy
//  }

  implicit val remoteDatabaseProxy: Future[RemoteDatabaseProxy] =
    Future.successful(new ApiBackedRemoteDatabaseProxy)
}

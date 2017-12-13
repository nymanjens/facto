package models.access

import api.ScalaJsApiClient
import models.user.User

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class Module(user: User)(implicit scalaJsApiClient: ScalaJsApiClient) {

  private lazy val localDatabase: Future[LocalDatabase] =
    LocalDatabase.createFuture(encryptionSecret = user.databaseEncryptionKey)
  implicit val remoteDatabaseProxy: Future[RemoteDatabaseProxy] = async {
    val db = await(localDatabase)
    val proxy = await(RemoteDatabaseProxy.create(scalaJsApiClient, db))

    proxy.startSchedulingModifiedEntityUpdates()

    proxy
  }
}

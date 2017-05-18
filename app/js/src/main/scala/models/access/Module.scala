package models.access

import scala.async.Async.{async, await}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future

object Module {

  import com.softwaremill.macwire._
  import api.Module._

  private lazy val localDatabase: Future[LocalDatabase] = LocalDatabase.createFuture()
  implicit val remoteDatabaseProxy: Future[RemoteDatabaseProxy] = async {
    val db = await(localDatabase)
    val proxy = await(RemoteDatabaseProxy.create(scalaJsApiClient, db))

    proxy.startSchedulingModifiedEntityUpdates() // Disabled for testing
    //    await(proxy.updateModifiedEntities()) // REMOVE THIS IN PROD (slows down startup for no good reason)

    proxy
  }
}

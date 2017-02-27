package models.access

import scala.concurrent.Future

object Module {

  import com.softwaremill.macwire._
  import api.Module._

  private lazy val localDatabase: Future[LocalDatabase] = LocalDatabase.createFuture()
  implicit val remoteDatabaseProxy: RemoteDatabaseProxy = wire[RemoteDatabaseProxy.Impl]
  //  remoteDatabaseProxy.startSchedulingModifiedEntityUpdates() // Disabled for testing
}

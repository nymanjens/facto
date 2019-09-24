package app.models.access

import app.models.user.User
import hydro.models.access.DbQueryExecutor
import hydro.models.access.DbResultSet
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.JsEntityAccessImpl
import hydro.models.access.RemoteDatabaseProxy

import scala.collection.immutable.Seq

private[access] final class AppJsEntityAccessImpl(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy,
    hydroPushSocketClientFactory: HydroPushSocketClientFactory,
) extends JsEntityAccessImpl
    with AppJsEntityAccess {

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))
}

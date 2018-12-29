package app.models.access

import app.models.user.User

import scala.collection.immutable.Seq

private[access] final class AppJsEntityAccessImpl(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy,
    entityModificationPushClientFactory: EntityModificationPushClientFactory)
    extends JsEntityAccessImpl
    with AppJsEntityAccess {

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))
}

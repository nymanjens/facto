package app.models.access

import app.models.user.User

import scala.collection.immutable.Seq

final class AppJsEntityAccess(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy,
    entityModificationPushClientFactory: EntityModificationPushClientFactory)
    extends JsEntityAccessImpl
    with AppEntityAccess {

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))
}

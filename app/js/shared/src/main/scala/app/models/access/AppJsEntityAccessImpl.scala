package app.models.access

import app.models.user.User
import hydro.models.access.RemoteDatabaseProxy
import scala.collection.immutable.Seq
import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction
import app.models.modification.EntityType.BalanceCheckType
import app.models.modification.EntityType.ExchangeRateMeasurementType
import app.models.modification.EntityType.TransactionGroupType
import app.models.modification.EntityType.TransactionType
import app.models.modification.EntityType.UserType
import app.models.user.User
import hydro.models.access.EntityModificationPushClientFactory
import hydro.models.access.HybridRemoteDatabaseProxy
import hydro.models.access.JsEntityAccess
import hydro.models.access.JsEntityAccessImpl
import hydro.models.access.LocalDatabaseImpl
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction
import hydro.models.access.DbResultSet
import hydro.models.access.DbQueryExecutor

import scala.collection.immutable.Seq

private[access] final class AppJsEntityAccessImpl(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy,
    entityModificationPushClientFactory: EntityModificationPushClientFactory)
    extends JsEntityAccessImpl
    with AppJsEntityAccess {

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))
}

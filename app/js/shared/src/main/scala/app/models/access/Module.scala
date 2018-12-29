package app.models.access

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

import scala.collection.immutable.Seq

final class Module(implicit user: User,
                   scalaJsApiClient: ScalaJsApiClient,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit val secondaryIndexFunction = Module.secondaryIndexFunction
  implicit val entityModificationPushClientFactory: EntityModificationPushClientFactory =
    new EntityModificationPushClientFactory()

  implicit val entityAccess: AppJsEntityAccess = {
    val webWorkerModule = new hydro.models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create()
    implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabaseFuture)
    val entityAccess = new AppJsEntityAccessImpl(getInitialDataResponse.allUsers)

    entityAccess.startCheckingForModifiedEntityUpdates()

    entityAccess
  }
}
object Module {
  val secondaryIndexFunction: SecondaryIndexFunction = SecondaryIndexFunction({
    case TransactionType =>
      Seq(
        ModelFields.Transaction.transactionGroupId,
        ModelFields.Transaction.moneyReservoirCode,
        ModelFields.Transaction.beneficiaryAccountCode)
    case TransactionGroupType        => Seq()
    case BalanceCheckType            => Seq()
    case ExchangeRateMeasurementType => Seq()
    case UserType                    => Seq()
  })
}

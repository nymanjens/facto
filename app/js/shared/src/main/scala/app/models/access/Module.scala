package app.models.access

import scala.collection.immutable.Seq
import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction
import app.models.accounting.BalanceCheck
import app.models.money.ExchangeRateMeasurement
import app.models.accounting.TransactionGroup
import app.models.accounting.Transaction

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
    case Transaction.Type =>
      Seq(
        ModelFields.Transaction.transactionGroupId,
        ModelFields.Transaction.moneyReservoirCode,
        ModelFields.Transaction.beneficiaryAccountCode)
    case TransactionGroup.Type        => Seq()
    case BalanceCheck.Type            => Seq()
    case ExchangeRateMeasurement.Type => Seq()
    case User.Type                    => Seq()
  })
}

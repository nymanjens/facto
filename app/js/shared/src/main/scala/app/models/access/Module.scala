package app.models.access

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.modification.EntityTypes
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.time.Clock
import hydro.models.access.EntitySyncLogic
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.HybridRemoteDatabaseProxy
import hydro.models.access.LocalDatabaseImpl
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction

import scala.collection.immutable.Seq

final class Module(implicit
    user: User,
    clock: Clock,
    scalaJsApiClient: ScalaJsApiClient,
    getInitialDataResponse: GetInitialDataResponse,
) {

  implicit private val secondaryIndexFunction = Module.secondaryIndexFunction
  implicit private val entitySyncLogic = new EntitySyncLogic.FullySynced(EntityTypes.all)

  implicit val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
    new HydroPushSocketClientFactory()

  implicit val entityAccess: AppJsEntityAccess = {
    val webWorkerModule = new hydro.models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create(separateDbPerCollection = false)
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
        ModelFields.Transaction.beneficiaryAccountCode,
      )
    case TransactionGroup.Type        => Seq()
    case BalanceCheck.Type            => Seq()
    case ExchangeRateMeasurement.Type => Seq()
    case User.Type                    => Seq()
  })
}

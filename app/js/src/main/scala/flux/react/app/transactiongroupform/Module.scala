package flux.react.app.transactiongroupform

import api.ScalaJsApi.GetInitialDataResponse
import common.I18n
import common.testing.TestObjects
import models.User
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy) {
  private val modelsModule = new models.Module

  import com.softwaremill.macwire._
  import common.time.Module._
  import flux.action.Module._
  import modelsModule.jsUserManager
  import modelsModule.jsTransactionManager
  import modelsModule.jsTransactionGroupManager
  import modelsModule.jsBalanceCheckManager
  import modelsModule.jsExchangeRateMeasurementManager
  import modelsModule.entityAccess
  import modelsModule.exchangeRateManager

  implicit private lazy val transactionPanel: TransactionPanel = wire[TransactionPanel]
  implicit private lazy val addTransactionPanel: AddTransactionPanel = wire[AddTransactionPanel]
  implicit private lazy val totalFlowRestrictionInput: TotalFlowRestrictionInput =
    wire[TotalFlowRestrictionInput]
  implicit private lazy val totalFlowInput: TotalFlowInput = wire[TotalFlowInput]
  implicit lazy val transactionGroupForm: TransactionGroupForm = wire[TransactionGroupForm]
}

package flux.react.app

import api.ScalaJsApi.GetInitialDataResponse
import common.I18n
import common.testing.TestObjects
import flux.react.app.transactiongroupform.TransactionGroupForm
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
  private val fluxStoresModule = new flux.stores.Module
  private val transactionGroupFormModule = new flux.react.app.transactiongroupform.Module

  import com.softwaremill.macwire._
  import common.time.Module._
  import modelsModule.entityAccess
  import modelsModule.exchangeRateManager
  import fluxStoresModule.allEntriesStoreFactory

  implicit lazy val menu: Menu = wire[Menu]
  implicit lazy val everything: Everything = wire[Everything]
  implicit lazy val transactionGroupForm: TransactionGroupForm = transactionGroupFormModule.transactionGroupForm
}

package flux.react.app

import api.ScalaJsApi.GetInitialDataResponse
import common.I18n
import common.testing.TestObjects
import flux.react.app.transactiongroupform.TransactionGroupForm
import flux.react.app.transactionviews.Everything
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
  private val transactionViewsModule = new flux.react.app.transactionviews.Module

  import com.softwaremill.macwire._
  import common.time.Module._
  import modelsModule.entityAccess
  import modelsModule.exchangeRateManager
  import fluxStoresModule.allEntriesStoreFactory
  import fluxStoresModule.globalMessagesStore

  implicit private lazy val menu: Menu = wire[Menu]
  implicit private lazy val globalMessages: GlobalMessages = wire[GlobalMessages]
  implicit lazy val layout: Layout = wire[Layout]
  implicit lazy val transactionGroupForm: TransactionGroupForm =
    transactionGroupFormModule.transactionGroupForm
  implicit lazy val everything = transactionViewsModule.everything
  implicit lazy val endowments = transactionViewsModule.endowments
}

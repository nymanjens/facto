package flux.react.app.transactionviews

import common.I18n
import flux.react.app.transactiongroupform.TransactionGroupForm
import models.User
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config

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

  implicit lazy val everything: Everything = wire[Everything]
}

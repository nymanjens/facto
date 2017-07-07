package flux.stores

import common.I18n
import flux.stores.entries.{AllEntriesStoreFactory, EndowmentEntriesStoreFactory}
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

final class Module(implicit i18n: I18n, remoteDatabaseProxy: RemoteDatabaseProxy, accountingConfig: Config) {

  private val modelsModule = new models.Module

  import com.softwaremill.macwire._
  import common.time.Module._
  import flux.action.Module._
  import modelsModule._

  implicit val allEntriesStoreFactory = wire[AllEntriesStoreFactory]
  implicit val endowmentEntriesStoreFactory = wire[EndowmentEntriesStoreFactory]
  implicit val transactionAndGroupStore = wire[TransactionAndGroupStore]
  implicit val globalMessagesStore = wire[GlobalMessagesStore]
}

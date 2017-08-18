package flux.stores

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.entries.{
  AllEntriesStoreFactory,
  CashFlowEntriesStoreFactory,
  EndowmentEntriesStoreFactory,
  LiquidationEntriesStoreFactory
}
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config
import models.accounting.money._
import models.{EntityAccess, User}

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  import com.softwaremill.macwire._

  wire[TransactionAndGroupStore]
  wire[BalanceCheckStore]

  implicit val allEntriesStoreFactory = wire[AllEntriesStoreFactory]
  implicit val cashFlowEntriesStoreFactory = wire[CashFlowEntriesStoreFactory]
  implicit val liquidationEntriesStoreFactory = wire[LiquidationEntriesStoreFactory]
  implicit val endowmentEntriesStoreFactory = wire[EndowmentEntriesStoreFactory]
  implicit val globalMessagesStore = wire[GlobalMessagesStore]
}

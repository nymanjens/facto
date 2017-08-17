package flux.react.app.transactionviews

import common.I18n
import common.time.Clock
import flux.stores.GlobalMessagesStore
import flux.stores.entries.{AllEntriesStoreFactory, CashFlowEntriesStoreFactory, EndowmentEntriesStoreFactory, LiquidationEntriesStoreFactory}
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager
import models.{EntityAccess, User}

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit lazy val everything = wire[Everything]
  implicit lazy val cashFlow = wire[CashFlow]
  implicit lazy val liquidation = wire[Liquidation]
  implicit lazy val endowments = wire[Endowments]
}

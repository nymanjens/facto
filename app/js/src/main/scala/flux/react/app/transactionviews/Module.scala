package flux.react.app.transactionviews

import common.I18n
import common.time.Clock
import flux.react.app.transactiongroupform.TransactionGroupForm
import flux.stores.GlobalMessagesStore
import flux.stores.entries.{
  AllEntriesStoreFactory,
  EndowmentEntriesStoreFactory,
  LiquidationEntriesStoreFactory
}
import models.{EntityAccess, User}
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit lazy val everything = wire[Everything]
  implicit lazy val endowments = wire[Endowments]
  implicit lazy val liquidation = wire[Liquidation]
}

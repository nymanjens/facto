package flux.react.app.transactionviews

import common.I18n
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.GlobalMessagesStore
import flux.stores.entries._
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config
import models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   dispatcher: Dispatcher,
                   exchangeRateManager: ExchangeRateManager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   complexQueryStoreFactory: ComplexQueryStoreFactory,
                   summaryForYearStoreFactory: SummaryForYearStoreFactory,
                   summaryYearsStoreFactory: SummaryYearsStoreFactory,
                   summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit lazy val everything = wire[Everything]
  implicit lazy val cashFlow = wire[CashFlow]
  implicit lazy val liquidation = wire[Liquidation]
  implicit lazy val endowments = wire[Endowments]
  implicit lazy val searchResults = wire[SearchResults]
  implicit private lazy val summaryTable = wire[SummaryTable]
  implicit lazy val summary = wire[Summary]
}

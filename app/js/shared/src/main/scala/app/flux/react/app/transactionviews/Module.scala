package app.flux.react.app.transactionviews

import common.I18n
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.entries.factories._
import hydro.flux.react.uielements.PageHeader
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: AppJsEntityAccess,
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
                   clock: Clock,
                   pageHeader: PageHeader,
) {

  implicit lazy val everything = new Everything
  implicit lazy val cashFlow = new CashFlow
  implicit lazy val liquidation = new Liquidation
  implicit lazy val endowments = new Endowments
  implicit lazy val searchResults = new SearchResults
  implicit private lazy val summaryTable = new SummaryTable
  implicit lazy val summary = new Summary
}

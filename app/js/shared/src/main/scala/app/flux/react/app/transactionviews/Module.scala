package app.flux.react.app.transactionviews

import app.common.accounting.TemplateMatcher
import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.InMemoryUserConfigStore
import app.flux.stores.CollapsedExpandedStateStoreFactory
import app.flux.stores.entries.factories._
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader

final class Module(implicit
    i18n: I18n,
    accountingConfig: Config,
    user: User,
    entityAccess: AppJsEntityAccess,
    dispatcher: Dispatcher,
    currencyValueManager: CurrencyValueManager,
    allEntriesStoreFactory: AllEntriesStoreFactory,
    cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
    liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
    endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
    complexQueryStoreFactory: ComplexQueryStoreFactory,
    summaryForYearStoreFactory: SummaryForYearStoreFactory,
    summaryYearsStoreFactory: SummaryYearsStoreFactory,
    summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
    summaryInflationGainsStoreFactory: SummaryInflationGainsStoreFactory,
    chartStoreFactory: ChartStoreFactory,
    collapsedExpandedStateStoreFactory: CollapsedExpandedStateStoreFactory,
    globalMessagesStore: GlobalMessagesStore,
    inMemoryUserConfigStore: InMemoryUserConfigStore,
    clock: Clock,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
    templateMatcher: TemplateMatcher,
    tagsStoreFactory: TagsStoreFactory,
) {

  implicit lazy val everything = new Everything
  implicit lazy val cashFlow = new CashFlow
  implicit lazy val liquidation = new Liquidation
  implicit lazy val endowments = new Endowments
  implicit private lazy val searchResultsEditAllPanel = new SearchResultsEditAllPanel
  implicit lazy val searchResults = new SearchResults
  implicit private lazy val summaryTable = new SummaryTable
  implicit lazy val summary = new Summary
  implicit private lazy val chartSpecInput = new ChartSpecInput()
  implicit lazy val chart = new Chart
}

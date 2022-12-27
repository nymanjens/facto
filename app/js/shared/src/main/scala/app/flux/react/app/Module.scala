package app.flux.react.app

import app.common.accounting.TemplateMatcher
import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.react.app.balancecheckform.BalanceCheckForm
import app.flux.react.app.transactiongroupform.TransactionGroupForm
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores._
import app.flux.stores.entries.factories._
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.LocalDatabaseHasBeenLoadedStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import hydro.flux.stores.DatabaseExplorerStoreFactory

final class Module(implicit
                   i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: AppJsEntityAccess,
                   currencyValueManager: CurrencyValueManager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   tagsStoreFactory: TagsStoreFactory,
                   complexQueryStoreFactory: ComplexQueryStoreFactory,
                   summaryForYearStoreFactory: SummaryForYearStoreFactory,
                   summaryYearsStoreFactory: SummaryYearsStoreFactory,
                   summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
                   summaryInflationGainsStoreFactory: SummaryInflationGainsStoreFactory,
                   chartStoreFactory: ChartStoreFactory,
                   collapsedExpandedStateStoreFactory: CollapsedExpandedStateStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   inMemoryUserConfigStore: InMemoryUserConfigStore,
                   pageLoadingStateStore: PageLoadingStateStore,
                   pendingModificationsStore: PendingModificationsStore,
                   applicationIsOnlineStore: ApplicationIsOnlineStore,
                   localDatabaseHasBeenLoadedStore: LocalDatabaseHasBeenLoadedStore,
                   userStore: UserStore,
                   databaseExplorerStoreFactory: DatabaseExplorerStoreFactory,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   templateMatcher: TemplateMatcher,
) {

  // Configuration of submodules
  private val hydroUielementsModule = new hydro.flux.react.uielements.Module
  implicit private lazy val pageHeader = hydroUielementsModule.pageHeader
  implicit private lazy val sbadminMenu = hydroUielementsModule.sbadminMenu
  implicit private lazy val sbadminLayout = hydroUielementsModule.sbadminLayout
  private val appUielementsModule = new app.flux.react.uielements.Module
  implicit private lazy val descriptionWithEntryCount = appUielementsModule.descriptionWithEntryCount

  private val userManagementModule = new hydro.flux.react.uielements.usermanagement.Module
  private val databaseExplorerModule = new hydro.flux.react.uielements.dbexplorer.Module
  private val transactionGroupFormModule = new app.flux.react.app.transactiongroupform.Module
  private val balanceCheckFormModule = new app.flux.react.app.balancecheckform.Module
  private val transactionViewsModule = new app.flux.react.app.transactionviews.Module

  implicit private lazy val menu: Menu = new Menu
  implicit private lazy val inflationToggleButton: InflationToggleButton = new InflationToggleButton

  implicit lazy val layout: Layout = new Layout

  implicit lazy val userProfile = userManagementModule.userProfile
  implicit lazy val userAdministration = userManagementModule.userAdministration
  implicit lazy val databaseExplorer = databaseExplorerModule.databaseExplorer

  implicit lazy val templateList = new TemplateList

  implicit lazy val transactionGroupForm: TransactionGroupForm =
    transactionGroupFormModule.transactionGroupForm
  implicit lazy val balanceCheckForm: BalanceCheckForm = balanceCheckFormModule.balanceCheckForm

  implicit lazy val everything = transactionViewsModule.everything
  implicit lazy val cashFlow = transactionViewsModule.cashFlow
  implicit lazy val liquidation = transactionViewsModule.liquidation
  implicit lazy val endowments = transactionViewsModule.endowments
  implicit lazy val searchResults = transactionViewsModule.searchResults
  implicit lazy val summary = transactionViewsModule.summary

  implicit lazy val chart = transactionViewsModule.chart
}

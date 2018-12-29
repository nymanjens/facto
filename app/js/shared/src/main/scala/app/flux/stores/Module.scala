package app.flux.stores

import app.api.ScalaJsApiClient
import common.I18n
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import app.flux.stores.entries._
import app.flux.stores.entries.factories._
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import app.models.access.EntityModificationPushClientFactory
import app.models.access.AppJsEntityAccess
import app.models.access.JsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: AppJsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   scalaJsApiClient: ScalaJsApiClient,
                   entityModificationPushClientFactory: EntityModificationPushClientFactory) {

  new TransactionAndGroupStore
  new BalanceCheckStore

  implicit private val complexQueryFilter = new ComplexQueryFilter

  implicit val allEntriesStoreFactory = new AllEntriesStoreFactory
  implicit val cashFlowEntriesStoreFactory = new CashFlowEntriesStoreFactory
  implicit val liquidationEntriesStoreFactory = new LiquidationEntriesStoreFactory
  implicit val endowmentEntriesStoreFactory = new EndowmentEntriesStoreFactory
  implicit val tagsStoreFactory = new TagsStoreFactory
  implicit val complexQueryStoreFactory = new ComplexQueryStoreFactory
  implicit val summaryForYearStoreFactory = new SummaryForYearStoreFactory
  implicit val summaryYearsStoreFactory = new SummaryYearsStoreFactory
  implicit val summaryExchangeRateGainsStoreFactory = new SummaryExchangeRateGainsStoreFactory
  implicit val globalMessagesStore = new GlobalMessagesStore
  implicit val pageLoadingStateStore = new PageLoadingStateStore
  implicit val pendingModificationsStore = new PendingModificationsStore
  implicit val applicationIsOnlineStore = new ApplicationIsOnlineStore
  implicit val userStore = new UserStore
}

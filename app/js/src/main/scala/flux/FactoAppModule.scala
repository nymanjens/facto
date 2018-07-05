package flux

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import flux.react.router.Page
import japgolly.scalajs.react.extra.router.Router
import models.access.JsEntityAccess
import models.accounting.config.Config
import models.user.User

import scala.async.Async.await

final class FactoAppModule(implicit getInitialDataResponse: GetInitialDataResponse,
                           scalaJsApiClient: ScalaJsApiClient) {

  // Unpack arguments
  implicit private val accountingConfig: Config = getInitialDataResponse.accountingConfig
  implicit private val user: User = getInitialDataResponse.user

  // Create and unpack common modules
  private val commonTimeModule = new common.time.Module
  implicit private val clock = commonTimeModule.clock
  private val commonModule = new common.Module
  implicit private val i18n = commonModule.i18n

  // Create and unpack Models Access module
  val modelsAccessModule = new models.access.Module
  implicit val entityAccess = modelsAccessModule.entityAccess
  implicit val entityModificationPushClientFactory = modelsAccessModule.entityModificationPushClientFactory

  // Create and unpack Models module
  private val modelsModule = new models.Module
  implicit private val exchangeRateManager = modelsModule.exchangeRateManager

  // Create and unpack Flux action module
  private val fluxActionModule = new flux.action.Module
  implicit private val dispatcher = fluxActionModule.dispatcher

  // Create and unpack Flux store module
  private val fluxStoresModule = new flux.stores.Module
  implicit private val allEntriesStoreFactory = fluxStoresModule.allEntriesStoreFactory
  implicit private val cashFlowEntriesStoreFactory = fluxStoresModule.cashFlowEntriesStoreFactory
  implicit private val liquidationEntriesStoreFactory = fluxStoresModule.liquidationEntriesStoreFactory
  implicit private val endowmentEntriesStoreFactory = fluxStoresModule.endowmentEntriesStoreFactory
  implicit private val tagsStoreFactory = fluxStoresModule.tagsStoreFactory
  implicit private val complexQueryStoreFactory = fluxStoresModule.complexQueryStoreFactory
  implicit private val summaryForYearStoreFactory = fluxStoresModule.summaryForYearStoreFactory
  implicit private val summaryYearsStoreFactory = fluxStoresModule.summaryYearsStoreFactory
  implicit private val summaryExchangeRateGainsStoreFactory =
    fluxStoresModule.summaryExchangeRateGainsStoreFactory
  implicit private val globalMessagesStore = fluxStoresModule.globalMessagesStore
  implicit private val pageLoadingStateStore = fluxStoresModule.pageLoadingStateStore
  implicit private val pendingModificationsStore = fluxStoresModule.pendingModificationsStore
  implicit private val applicationIsOnlineStore = fluxStoresModule.applicationIsOnlineStore
  implicit private val userStore = fluxStoresModule.userStore

  // Create other Flux modules
  implicit private val reactAppModule = new flux.react.app.Module
  implicit private val routerModule = new flux.react.router.Module

  val router: Router[Page] = routerModule.router
}

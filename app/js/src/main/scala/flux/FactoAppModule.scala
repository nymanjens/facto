package flux

import api.ScalaJsApi.GetInitialDataResponse
import flux.react.router.Page
import japgolly.scalajs.react.extra.router.Router
import models.User
import models.access.RemoteDatabaseProxy
import models.accounting.config.Config

final class FactoAppModule(implicit getInitialDataResponse: GetInitialDataResponse,
                           remoteDatabaseProxy: RemoteDatabaseProxy) {

  implicit private val accountingConfig: Config = getInitialDataResponse.accountingConfig
  implicit private val user: User = getInitialDataResponse.user

  private val commonModule = new common.Module
  implicit private val i18n = commonModule.i18n

  implicit private val reactAppModule = new flux.react.app.Module
  implicit private val routerModule = new flux.react.router.Module

  val router: Router[Page] = routerModule.router
}

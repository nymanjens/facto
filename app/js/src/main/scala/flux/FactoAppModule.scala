package flux

import api.ScalaJsApi.GetInitialDataResponse
import flux.react.router.RouterFactory$
import models.User
import models.accounting.config.Config

final class FactoAppModule(implicit getInitialDataResponse: GetInitialDataResponse) {

  implicit private val accountingConfig: Config = getInitialDataResponse.accountingConfig
  implicit private val user: User = getInitialDataResponse.user

  private val commonModule = new common.Module
  implicit private val i18n = commonModule.i18n

  implicit private val reactAppModule = new flux.react.app.Module
  implicit private val routerModule = new flux.react.router.Module

  val routerFactory: RouterFactory = routerModule.routerFactory
}

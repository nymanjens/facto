package flux.react.router

import api.ScalaJsApi.GetInitialDataResponse
import models.User
import models.accounting.config.Config

final class Module(implicit reactAppModule: flux.react.app.Module) {

  import com.softwaremill.macwire._

  implicit lazy val routerConfig: RouterConfig = wire[RouterConfig.Impl]
}

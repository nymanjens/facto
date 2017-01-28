package flux.react.app

import api.ScalaJsApi.GetInitialDataResponse
import common.testing.TestObjects
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

final class Module(implicit getInitialDataResponse: GetInitialDataResponse) {

  import com.softwaremill.macwire._
  import common.time.Module._
  import models.access.Module._
  import models.Module._
  import flux.stores.Module._

  val commonModule = new common.Module
  implicit val i18n = commonModule.i18n

  implicit val accountingConfig: Config = getInitialDataResponse.accountingConfig
  implicit val user: User = getInitialDataResponse.user
  implicit lazy val everything: Everything = wire[Everything]
}

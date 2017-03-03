package flux.react.app

import api.ScalaJsApi.GetInitialDataResponse
import common.I18n
import common.testing.TestObjects
import flux.react.app.transactiongroupform.TransactionGroupForm
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

final class Module(implicit i18n: I18n, accountingConfig: Config) {

  import com.softwaremill.macwire._
  import common.time.Module._
  import models.access.Module._
  import models.Module._
  import flux.stores.Module._

  implicit lazy val menu: Menu = wire[Menu]
  implicit lazy val everything: Everything = wire[Everything]
  implicit lazy val transactionGroupForm: TransactionGroupForm = wire[TransactionGroupForm]
}

package flux.react.app

import common.testing.TestObjects
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

object Module {

  import com.softwaremill.macwire._
  import common.Module._
  import common.time.Module._
  import models.access.Module._
  import models.Module._
  import flux.stores.Module._

  // TODO: Provide accounting Config
  implicit val accountingConfig: Config =  TestObjects.testAccountingConfig
  implicit lazy val everything: Everything = wire[Everything]
}

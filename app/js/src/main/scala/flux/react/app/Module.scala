package flux.react.app

import models.accounting._
import models.accounting.money._

object Module {

  import com.softwaremill.macwire._
  import common.Module._
  import models.access.Module._
  import models.Module._
  import flux.stores.Module._

  implicit lazy val everything: Everything = wire[Everything]
}

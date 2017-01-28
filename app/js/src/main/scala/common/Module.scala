package common

import common.testing.FakeI18n

object Module {

  import com.softwaremill.macwire._

  // TODO: Implement I18n
  implicit lazy val i18n: I18n = wire[FakeI18n]
}

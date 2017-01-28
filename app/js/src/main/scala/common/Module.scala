package common

import api.ScalaJsApi.GetInitialDataResponse
import common.testing.FakeI18n

final class Module(implicit getInitialDataResponse: GetInitialDataResponse) {

  import com.softwaremill.macwire._

  // TODO: Implement I18n
  implicit lazy val i18n: I18n = wire[FakeI18n]
}

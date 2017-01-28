package common

import api.ScalaJsApi.GetInitialDataResponse
import common.testing.FakeI18n

final class Module(implicit getInitialDataResponse: GetInitialDataResponse) {

  import com.softwaremill.macwire._

  implicit lazy val i18n: I18n = new JsI18n(getInitialDataResponse.i18nMessages)
}

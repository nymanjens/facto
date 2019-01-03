package hydro.common

import app.api.ScalaJsApi.GetInitialDataResponse
import hydro.common.I18n

final class Module(implicit getInitialDataResponse: GetInitialDataResponse) {

  implicit lazy val i18n: I18n = new JsI18n(getInitialDataResponse.i18nMessages)
}

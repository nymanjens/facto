package app.models

import app.api.ScalaJsApi.GetInitialDataResponse
import common.money.ExchangeRateManager
import app.models.access.JsEntityAccess
import app.models.money.JsExchangeRateManager

final class Module(implicit entityAccess: JsEntityAccess, getInitialDataResponse: GetInitialDataResponse) {

  implicit lazy val exchangeRateManager: ExchangeRateManager =
    new JsExchangeRateManager(getInitialDataResponse.ratioReferenceToForeignCurrency)
}

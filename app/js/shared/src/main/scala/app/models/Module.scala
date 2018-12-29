package app.models

import app.api.ScalaJsApi.GetInitialDataResponse
import common.money.ExchangeRateManager
import app.models.access.AppJsEntityAccess
import app.models.money.JsExchangeRateManager

final class Module(implicit entityAccess: AppJsEntityAccess, getInitialDataResponse: GetInitialDataResponse) {

  implicit lazy val exchangeRateManager: ExchangeRateManager =
    new JsExchangeRateManager(getInitialDataResponse.ratioReferenceToForeignCurrency)
}

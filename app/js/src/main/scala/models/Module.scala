package models

import api.ScalaJsApi.GetInitialDataResponse
import common.money.ExchangeRateManager
import models.access.JsEntityAccess
import models.money.JsExchangeRateManager

final class Module(implicit entityAccess: JsEntityAccess, getInitialDataResponse: GetInitialDataResponse) {

  implicit lazy val exchangeRateManager: ExchangeRateManager =
    new JsExchangeRateManager(getInitialDataResponse.ratioReferenceToForeignCurrency)
}

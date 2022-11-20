package app.models

import hydro.common.time.Clock
import app.api.ScalaJsApi.GetInitialDataResponse
import app.common.money.ExchangeRateManager
import app.models.access.AppJsEntityAccess
import app.models.money.JsExchangeRateManager

final class Module(implicit
    entityAccess: AppJsEntityAccess,
    getInitialDataResponse: GetInitialDataResponse,
    clock: Clock,
) {

  implicit lazy val exchangeRateManager: ExchangeRateManager =
    new JsExchangeRateManager(getInitialDataResponse.ratioReferenceToForeignCurrency)
}

package app.models

import hydro.common.time.Clock
import app.api.ScalaJsApi.GetInitialDataResponse
import app.common.money.CurrencyValueManager
import app.models.access.AppJsEntityAccess
import app.models.money.JsCurrencyValueManager

final class Module(implicit
    entityAccess: AppJsEntityAccess,
    getInitialDataResponse: GetInitialDataResponse,
    clock: Clock,
) {

  implicit lazy val currencyValueManager: CurrencyValueManager =
    new JsCurrencyValueManager(getInitialDataResponse.ratioReferenceToForeignCurrency)
}

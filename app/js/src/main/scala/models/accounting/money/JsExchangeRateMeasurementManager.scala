package models.accounting.money

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

final class JsExchangeRateMeasurementManager(implicit database: RemoteDatabaseProxy)
  extends BaseJsEntityManager[ExchangeRateMeasurement]
    with ExchangeRateMeasurement.Manager {
  override def fetchAll(currency: Currency) = {
    database.newQuery[ExchangeRateMeasurement]()
      .find("foreignCurrencyCode" -> currency.code)
      .sort("data")
      .data()
  }
}

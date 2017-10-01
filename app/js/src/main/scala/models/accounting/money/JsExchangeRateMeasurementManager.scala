package models.accounting.money

import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala2js.Converters._
import scala2js.Keys

final class JsExchangeRateMeasurementManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[ExchangeRateMeasurement]
    with ExchangeRateMeasurement.Manager {
  override def fetchAll(currency: Currency) = {
    database
      .newQuery[ExchangeRateMeasurement]()
      .filterEqual(Keys.ExchangeRateMeasurement.foreignCurrencyCode, currency.code)
      .sort(LokiJs.Sorting.ascBy(Keys.ExchangeRateMeasurement.date))
      .data()
  }
}

package models.money

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala2js.Converters._

final class JsExchangeRateMeasurementManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[ExchangeRateMeasurement]
    with ExchangeRateMeasurement.Manager

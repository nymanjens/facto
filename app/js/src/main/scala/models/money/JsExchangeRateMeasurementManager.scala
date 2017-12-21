package models.money

import common.money.Currency
import jsfacades.LokiJs
import models.access.DbQuery
import models.access.DbQueryImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala2js.Converters._
import models.access.Fields

final class JsExchangeRateMeasurementManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[ExchangeRateMeasurement]
    with ExchangeRateMeasurement.Manager

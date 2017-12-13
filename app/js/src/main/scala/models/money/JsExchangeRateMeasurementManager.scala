package models.money

import common.money.Currency
import jsfacades.LokiJs
import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala2js.Converters._
import scala2js.Keys

final class JsExchangeRateMeasurementManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[ExchangeRateMeasurement]
    with ExchangeRateMeasurement.Manager

package models

import common.time.LocalDateTime
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.money._

final class Module(implicit remoteDatabaseProxy: RemoteDatabaseProxy) {

  import com.softwaremill.macwire._

  implicit lazy val jsUserManager = wire[JsUserManager]
  implicit lazy val jsTransactionManager = wire[JsTransactionManager]
  implicit lazy val jsTransactionGroupManager = wire[JsTransactionGroupManager]
  implicit lazy val jsBalanceCheckManager = wire[JsBalanceCheckManager]
  implicit lazy val jsExchangeRateMeasurementManager = wire[JsExchangeRateMeasurementManager]

  implicit lazy val entityAccess: EntityAccess = wire[JsEntityAccess]
  implicit lazy val exchangeRateManager: ExchangeRateManager = wire[JsExchangeRateManager]
}

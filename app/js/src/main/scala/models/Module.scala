package models

import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.money._

final class Module(implicit remoteDatabaseProxy: RemoteDatabaseProxy) {

  import com.softwaremill.macwire._

  implicit lazy val userManager = wire[JsUserManager]
  implicit lazy val transactionManager = wire[JsTransactionManager]
  implicit lazy val transactionGroupManager = wire[JsTransactionGroupManager]
  implicit lazy val balanceCheckManager = wire[JsBalanceCheckManager]
  implicit lazy val exchangeRateMeasurementManager = wire[JsExchangeRateMeasurementManager]

  implicit lazy val entityAccess: EntityAccess = wire[JsEntityAccess]
  implicit lazy val exchangeRateManager: ExchangeRateManager = wire[JsExchangeRateManager]
}

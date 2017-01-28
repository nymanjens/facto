package models

import common.time.LocalDateTime
import models.accounting._
import models.accounting.money._

object Module {

  import com.softwaremill.macwire._
  import models.access.Module._

  implicit lazy val jsUserManager = wire[JsUserManager]
  implicit lazy val jsTransactionManager = wire[JsTransactionManager]
  implicit lazy val jsTransactionGroupManager = wire[JsTransactionGroupManager]
  implicit lazy val jsBalanceCheckManager = wire[JsBalanceCheckManager]
  implicit lazy val jsExchangeRateMeasurementManager = wire[JsExchangeRateMeasurementManager]

  implicit lazy val entityAccess: EntityAccess = wire[JsEntityAccess]

  // TODO: implement ExchangeRateManager
  implicit lazy val exchangeRateManager: ExchangeRateManager = new ExchangeRateManager {
    override def getRatioSecondToFirstCurrency(firstCurrency: Currency, secondCurrency: Currency, date: LocalDateTime) = 1
  }
}

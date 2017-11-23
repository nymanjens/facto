package common.testing

import flux.action.Dispatcher
import models.money.JsExchangeRateMeasurementManager
import models.accounting.{JsBalanceCheckManager, JsTransactionGroupManager, JsTransactionManager}
import models.money.{JsExchangeRateManager, JsExchangeRateMeasurementManager}
import models.{JsEntityAccess, JsUserManager}

class TestModule {

  import com.softwaremill.macwire._

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeRemoteDatabaseProxy = wire[FakeRemoteDatabaseProxy]
  implicit lazy val fakeClock = wire[FakeClock]
  implicit lazy val fakeDispatcher = wire[Dispatcher.FakeSynchronous]
  implicit lazy val fakeI18n = wire[FakeI18n]
  implicit lazy val testAccountingConfig = TestObjects.testAccountingConfig
  implicit lazy val testUser = TestObjects.testUser

  // ******************* Non-fake implementations ******************* //
  implicit lazy val userManager = wire[JsUserManager]
  implicit lazy val transactionManager = wire[JsTransactionManager]
  implicit lazy val transactionGroupManager = wire[JsTransactionGroupManager]
  implicit lazy val balanceCheckManager = wire[JsBalanceCheckManager]
  implicit lazy val exchangeRateMeasurementManager = wire[JsExchangeRateMeasurementManager]

  implicit lazy val entityAccess = wire[JsEntityAccess]
  implicit lazy val exchangeRateManager = wire[JsExchangeRateManager]
}

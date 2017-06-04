package common.testing

import flux.action.Dispatcher
import models.{EntityAccess, JsEntityAccess, JsUserManager}
import models.accounting.money.{JsExchangeRateMeasurementManager, JsExchangeRateManager}
import models.accounting.{
  JsBalanceCheckManager,
  JsTransactionGroupManager,
  JsTransactionManager,
  Transaction
}

class TestModule {

  import com.softwaremill.macwire._

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeRemoteDataProxy = wire[FakeRemoteDatabaseProxy]
  implicit lazy val fakeClock = wire[FakeClock]
  implicit lazy val fakeDispatcher = wire[Dispatcher.FakeSynchronous]
  implicit lazy val fakeI18n = wire[FakeI18n]
  implicit lazy val accountingConfig = TestObjects.testAccountingConfig

  // ******************* Non-fake implementations ******************* //
  implicit lazy val jsUserManager = wire[JsUserManager]
  implicit lazy val jsTransactionManager = wire[JsTransactionManager]
  implicit lazy val jsTransactionGroupManager = wire[JsTransactionGroupManager]
  implicit lazy val jsBalanceCheckManager = wire[JsBalanceCheckManager]
  implicit lazy val jsExchangeRateMeasurementManager = wire[JsExchangeRateMeasurementManager]

  implicit lazy val entityAccess = wire[JsEntityAccess]
  implicit lazy val exchangeRateManager = wire[JsExchangeRateManager]
}

package common.testing

import flux.action.Dispatcher
import models.money.JsExchangeRateManager

class TestModule {

  import com.softwaremill.macwire._

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeEntityAccess = wire[FakeJsEntityAccess]
  implicit lazy val fakeClock = wire[FakeClock]
  implicit lazy val fakeDispatcher = wire[Dispatcher.FakeSynchronous]
  implicit lazy val fakeI18n = wire[FakeI18n]
  implicit lazy val testAccountingConfig = TestObjects.testAccountingConfig
  implicit lazy val testUser = TestObjects.testUser

  // ******************* Non-fake implementations ******************* //
  implicit lazy val exchangeRateManager = new JsExchangeRateManager(ratioReferenceToForeignCurrency = Map())
}

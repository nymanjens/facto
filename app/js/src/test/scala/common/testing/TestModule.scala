package common.testing

import flux.action.Dispatcher
import models.access.EntityModificationPushClientFactory
import models.money.JsExchangeRateManager

class TestModule {

  import com.softwaremill.macwire._

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeEntityAccess = new FakeJsEntityAccess
  implicit lazy val fakeClock = new FakeClock
  implicit lazy val fakeDispatcher = wire[Dispatcher.Fake]
  implicit lazy val fakeI18n = new FakeI18n
  implicit lazy val testAccountingConfig = TestObjects.testAccountingConfig
  implicit lazy val testUser = TestObjects.testUser
  implicit lazy val fakeScalaJsApiClient = new FakeScalaJsApiClient

  // ******************* Non-fake implementations ******************* //
  implicit lazy val exchangeRateManager = new JsExchangeRateManager(ratioReferenceToForeignCurrency = Map())
  implicit lazy val entityModificationPushClientFactory: EntityModificationPushClientFactory =
    new EntityModificationPushClientFactory
}

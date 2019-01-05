package app.common.testing

import app.models.money.JsExchangeRateManager
import hydro.common.testing.FakeClock
import hydro.common.testing.FakeI18n
import hydro.common.testing.FakeJsEntityAccess
import hydro.flux.action.Dispatcher
import hydro.models.access.EntityModificationPushClientFactory

class TestModule {

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeEntityAccess = new FakeJsEntityAccess
  implicit lazy val fakeClock = new FakeClock
  implicit lazy val fakeDispatcher = new Dispatcher.Fake
  implicit lazy val fakeI18n = new FakeI18n
  implicit lazy val testAccountingConfig = TestObjects.testAccountingConfig
  implicit lazy val testUser = TestObjects.testUser
  implicit lazy val fakeScalaJsApiClient = new FakeScalaJsApiClient

  // ******************* Non-fake implementations ******************* //
  implicit lazy val exchangeRateManager = new JsExchangeRateManager(ratioReferenceToForeignCurrency = Map())
  implicit lazy val entityModificationPushClientFactory: EntityModificationPushClientFactory =
    new EntityModificationPushClientFactory
}

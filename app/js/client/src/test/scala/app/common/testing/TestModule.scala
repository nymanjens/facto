package app.common.testing

import app.models.money.JsExchangeRateManager
import hydro.common.testing.FakeClock
import hydro.common.testing.FakeI18n
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader
import hydro.models.access.EntityModificationPushClientFactory

class TestModule {

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeEntityAccess = new FakeAppJsEntityAccess
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
  implicit val pageHeader = new PageHeader
}

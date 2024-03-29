package app.common.testing

import app.models.money.JsCurrencyValueManager
import hydro.common.testing.FakeClock
import hydro.common.testing.FakeI18n
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader
import hydro.models.access.HydroPushSocketClientFactory

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
  implicit lazy val currencyValueManager = new JsCurrencyValueManager(ratioReferenceToForeignCurrency = Map())
  implicit lazy val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
    new HydroPushSocketClientFactory
  implicit val pageHeader = new PageHeader
}

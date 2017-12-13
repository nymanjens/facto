package models.money

import java.time.Duration

import common.money.Currency.{Eur, Gbp, Usd}
import common.testing.TestModule
import common.time.LocalDateTime
import utest.{TestSuite, _}

import scala2js.Converters._

object JsExchangeRateManagerTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    val clock = testModule.fakeClock
    val remoteDatabaseProxy = testModule.fakeRemoteDatabaseProxy
    val exchangeRateManager: JsExchangeRateManager = testModule.exchangeRateManager

    "getRatioSecondToFirstCurrency()" - {
      persistGbpMeasurement(date = yesterdayPlusMillis(1000), ratio = 2)
      persistGbpMeasurement(date = yesterdayPlusMillis(2000), ratio = 3)
      persistGbpMeasurement(date = yesterdayPlusMillis(3000), ratio = 0.5)

      exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Eur, clock.now) ==> 1.0
      exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Usd, clock.now) ==> 1.0

      exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(0)) ==> 1.0
      exchangeRateManager
        .getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(999)) ==> 1.0
      exchangeRateManager
        .getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1000)) ==> 2.0
      exchangeRateManager
        .getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1001)) ==> 2.0
      exchangeRateManager
        .getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(2000)) ==> 3.0
      exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 0.5

      exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Gbp, clock.now) ==> 2.0

      persistGbpMeasurement(date = yesterdayPlusMillis(4000), ratio = 9)
      exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 9.0
    }

    def persistGbpMeasurement(date: LocalDateTime, ratio: Double): Unit = {
      remoteDatabaseProxy.addWithRandomId(
        ExchangeRateMeasurement(
          date = date,
          foreignCurrencyCode = Gbp.code,
          ratioReferenceToForeignCurrency = ratio))
    }

    def yesterdayPlusMillis(millis: Long): LocalDateTime = {
      clock.now.plus(Duration.ofDays(-1).plusMillis(millis))
    }
  }
}

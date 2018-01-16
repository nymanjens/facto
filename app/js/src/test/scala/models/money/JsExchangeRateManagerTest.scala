package models.money

import java.time.Duration

import common.money.Currency.{Eur, Gbp, Usd}
import common.testing.{FakeJsEntityAccess, TestModule}
import common.time.{Clock, LocalDateTime}
import models.access.{EntityAccess, JsEntityAccess}
import utest.{TestSuite, _}

import scala.collection.SortedMap
import scala2js.Converters._

object JsExchangeRateManagerTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val clock = testModule.fakeClock
    implicit val entityAccess = testModule.fakeEntityAccess
    val exchangeRateManager: JsExchangeRateManager =
      new JsExchangeRateManager(
        ratioReferenceToForeignCurrency = Map(
          Gbp -> SortedMap(
            yesterdayPlusMillis(1000) -> 2.0,
            yesterdayPlusMillis(2000) -> 3.0,
            yesterdayPlusMillis(3000) -> 0.5
          ),
        )
      )

    "getRatioSecondToFirstCurrency()" - {
      "without data" - {
        exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Eur, clock.now) ==> 1.0
        exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Usd, clock.now) ==> 1.0
      }
      "before first data point" - {
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(0)) ==> 1.0
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(999)) ==> 1.0
      }
      "at first data point" - {
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1000)) ==> 2.0
      }
      "after first data point" - {
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1001)) ==> 2.0
      }
      "at second data point" - {
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(2000)) ==> 3.0
      }
      "after last data point" - {
        "gbp / eur" - {
          exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 0.5
        }
        "eur / gbp" - {
          exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Gbp, clock.now) ==> 2.0
        }
      }
      "after change was persisted" - {
        persistGbpMeasurement(yesterdayPlusMillis(4000), 4.0)

        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(3999)) ==> 0.5
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(4000)) ==> 4.0
        exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 4.0
      }
    }

  }

  def persistGbpMeasurement(date: LocalDateTime, ratio: Double)(
      implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addWithRandomId(
      ExchangeRateMeasurement(
        date = date,
        foreignCurrencyCode = Gbp.code,
        ratioReferenceToForeignCurrency = ratio))
  }

  def yesterdayPlusMillis(millis: Long)(implicit clock: Clock): LocalDateTime = {
    clock.now.plus(Duration.ofDays(-1).plusMillis(millis))
  }
}

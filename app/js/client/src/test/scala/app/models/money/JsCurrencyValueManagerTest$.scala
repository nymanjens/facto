package app.models.money

import java.time.Duration

import app.common.money.Currency.Eur
import app.common.money.Currency.Gbp
import app.common.money.Currency.Usd
import hydro.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime
import utest.TestSuite
import utest._

import scala.collection.SortedMap

object JsCurrencyValueManagerTest$ extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val clock = testModule.fakeClock
    implicit val entityAccess = testModule.fakeEntityAccess
    val currencyValueManager: JsCurrencyValueManager =
      new JsCurrencyValueManager(
        ratioReferenceToForeignCurrency = Map(
          Gbp -> SortedMap(
            yesterdayPlusMillis(1000) -> 2.0,
            yesterdayPlusMillis(2000) -> 3.0,
            yesterdayPlusMillis(3000) -> 0.5,
          )
        )
      )

    "getRatioSecondToFirstCurrency()" - {
      "without data" - {
        currencyValueManager.getRatioSecondToFirstCurrency(Eur, Eur, clock.now) ==> 1.0
        currencyValueManager.getRatioSecondToFirstCurrency(Eur, Usd, clock.now) ==> 1.0
      }
      "before first data point" - {
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(0)) ==> 1.0
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(999)) ==> 1.0
      }
      "at first data point" - {
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1000)) ==> 2.0
      }
      "after first data point" - {
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(1001)) ==> 2.0
      }
      "at second data point" - {
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(2000)) ==> 3.0
      }
      "after last data point" - {
        "gbp / eur" - {
          currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 0.5
        }
        "eur / gbp" - {
          currencyValueManager.getRatioSecondToFirstCurrency(Eur, Gbp, clock.now) ==> 2.0
        }
      }
      "after change was persisted" - {
        persistGbpMeasurement(yesterdayPlusMillis(4000), 4.0)

        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(3999)) ==> 0.5
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, yesterdayPlusMillis(4000)) ==> 4.0
        currencyValueManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) ==> 4.0
      }
    }
  }

  def persistGbpMeasurement(date: LocalDateTime, ratio: Double)(implicit
      entityAccess: FakeJsEntityAccess
  ): Unit = {
    entityAccess.addWithRandomId(
      ExchangeRateMeasurement(
        date = date,
        foreignCurrencyCode = Gbp.code,
        ratioReferenceToForeignCurrency = ratio,
      )
    )
  }

  def yesterdayPlusMillis(millis: Long)(implicit clock: Clock): LocalDateTime = {
    clock.now.plus(Duration.ofDays(-1).plusMillis(millis))
  }
}

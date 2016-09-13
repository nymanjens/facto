package models.accounting.money

import common.Clock
import Currency.{Eur, Gbp, Usd}
import common.cache.CacheRegistry
import common.testing.CacheClearingSpecification
import common.testing.TestUtils.persistGbpMeasurement
import org.joda.time.DateTime
import org.specs2.mutable._
import play.api.test.WithApplication
import play.twirl.api.Html

class ExchangeRateManagerTest extends CacheClearingSpecification {

  "getRatioSecondToFirstCurrency()" in new WithApplication {
    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 0.5)

    ExchangeRateManager.getRatioSecondToFirstCurrency(Eur, Eur, DateTime.now) mustEqual 1.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Eur, Usd, DateTime.now) mustEqual 1.0

    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(0)) mustEqual 1.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(999)) mustEqual 1.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(1000)) mustEqual 2.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(1001)) mustEqual 2.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(2000)) mustEqual 3.0
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now) mustEqual 0.5

    ExchangeRateManager.getRatioSecondToFirstCurrency(Eur, Gbp, DateTime.now) mustEqual 2.0

    persistGbpMeasurement(millisSinceEpoch = 4000, ratio = 9)
    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now) mustEqual 9.0
  }

  "Verify consistency" in new WithApplication {
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 4)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    ExchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
  }
}

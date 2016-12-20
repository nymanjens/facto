package models.accounting.money

import com.google.inject._
import common.time.Clock
import Currency.{Eur, Gbp, Usd}
import common.cache.CacheRegistry
import common.testing._
import common.testing.TestUtils.persistGbpMeasurement
import java.time.Instant
import org.specs2.mutable._
import play.api.test.WithApplication
import play.twirl.api.Html
import models._
import common.testing.TestUtils._

class ExchangeRateManagerTest extends CacheClearingSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: EntityAccess = null

  @Inject private val exchangeRateManager: ExchangeRateManager = null

  override def beforeEveryTest() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "getRatioSecondToFirstCurrency()" in new WithApplication {
    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 0.5)

    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Eur, clock.now) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Usd, clock.now) mustEqual 1.0

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, localDateTimeOfEpochMilli(0)) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, localDateTimeOfEpochMilli(999)) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, localDateTimeOfEpochMilli(1000)) mustEqual 2.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, localDateTimeOfEpochMilli(1001)) mustEqual 2.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, localDateTimeOfEpochMilli(2000)) mustEqual 3.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) mustEqual 0.5

    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Gbp, clock.now) mustEqual 2.0

    persistGbpMeasurement(millisSinceEpoch = 4000, ratio = 9)
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now) mustEqual 9.0
  }

  "Verify consistency" in new WithApplication {
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 4)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, clock.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
  }
}

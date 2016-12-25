package models.accounting.money

import com.google.inject._
import common.testing.TestUtils.persistGbpMeasurement
import common.testing._
import common.time.Clock
import models._
import play.api.test.WithApplication
import play.twirl.api.Html

class MoneyTest extends CacheClearingSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject implicit private val exchangeRateManager: ExchangeRateManager = null

  override def beforeEveryTest() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "Money" should {
    "centsToFloatString" in {
      Money.centsToFloatString(-8701) mustEqual "-87.01"
    }

    "floatToCents" in {
      Money.floatToCents(81.234) mustEqual 8123
      Money.floatToCents(-1.234) mustEqual -123
      Money.floatToCents(0.236) mustEqual 24
    }
  }

  "ReferenceMoney" should {
    "+" in {
      ReferenceMoney(4) + ReferenceMoney(-5) mustEqual ReferenceMoney(-1)
    }

    "formatFloat" in {
      ReferenceMoney(0).formatFloat mustEqual "0.00"
      ReferenceMoney(87).formatFloat mustEqual "0.87"
      ReferenceMoney(987).formatFloat mustEqual "9.87"
      ReferenceMoney(-987).formatFloat mustEqual "-9.87"
      ReferenceMoney(-87).formatFloat mustEqual "-0.87"
      ReferenceMoney(-8701).formatFloat mustEqual "-87.01"
    }

    "toHtmlWithCurrency" in {
      ReferenceMoney(-987).toHtmlWithCurrency mustEqual "&euro; -9.87"
    }

    "withDate" in {
      val date = clock.now
      ReferenceMoney(-987).withDate(date) mustEqual DatedMoney(cents = -987, currency = Currency.default, date = date)
    }
  }


  "MoneyWithGeneralCurrency" should {
    "numeric" in {
      val numeric = MoneyWithGeneralCurrency.numeric(Currency.Gbp)

      Seq(
        MoneyWithGeneralCurrency(-111, Currency.Gbp),
        MoneyWithGeneralCurrency(-222, Currency.Gbp)
      ).sum(numeric) mustEqual MoneyWithGeneralCurrency(-333, Currency.Gbp)

      Seq(
        MoneyWithGeneralCurrency(-111, Currency.Eur),
        MoneyWithGeneralCurrency(-222, Currency.Gbp)
      ).sum(numeric) should throwA[IllegalArgumentException]
    }

    "toHtmlWithCurrency" in {
      MoneyWithGeneralCurrency(-987, Currency.Gbp).toHtmlWithCurrency mustEqual "&pound; -9.87"
    }

  }

  "DatedMoney" should {
    "exchangedForReferenceCurrency" in new WithApplication {
      persistGbpMeasurement(millisSinceEpoch = 0, ratio = 1.3)

      val money = DatedMoney(10, Currency.Gbp, clock.now)
      money.exchangedForReferenceCurrency mustEqual ReferenceMoney(13)
    }

    "exchangedForCurrency" in new WithApplication {
      persistGbpMeasurement(millisSinceEpoch = 0, ratio = 1.3)

      val date = clock.now
      val money = DatedMoney(10, Currency.Gbp, date)
      money.exchangedForCurrency(Currency.default) mustEqual DatedMoney(13, Currency.default, date)
    }

    "toHtmlWithCurrency" in new WithApplication {
      persistGbpMeasurement(millisSinceEpoch = 0, ratio = 1.3)

      DatedMoney(10, Currency.Gbp, clock.now).toHtmlWithCurrency mustEqual
        """&pound; 0.10 <span class="reference-currency">&euro; 0.13</span>"""
    }
  }
}

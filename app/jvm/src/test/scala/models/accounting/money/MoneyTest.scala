package models.accounting.money

import com.google.inject._
import common.testing.TestUtils.persistGbpMeasurement
import common.testing._
import common.time.Clock
import models._
import play.api.test.WithApplication
import play.twirl.api.Html

import scala.collection.immutable.Seq

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

    "floatStringToCents" in {
      case class Case(input: String, output: Option[Long]) {
        def negated: Case = Case("-" + input, output.map(cents => -cents))
        def withPlusSignPrefix: Case = Case("+" + input, output)

        def test = {
          val resultTry = Money.tryFloatStringToCents(input)
          if (resultTry.toOption != output) {
            throw new AssertionError(
              s"Money.tryFloatStringToCents('$input') = $resultTry, but expected $output")
          }
          resultTry.toOption mustEqual output
        }
      }
      val testCases = Seq(
        Case("1.23", Option(123)),
        Case("1.2", Option(120)),
        Case("1,23", Option(123)),
        Case("1,2", Option(120)),
        Case("12345.", Option(1234500)),
        Case(".12", Option(12)),
        Case(",12", Option(12)),
        Case("12,", Option(1200)),
        Case("12.", Option(1200)),
        Case("1,234,567.8", Option(123456780)),
        Case("1,234,567", Option(123456700)),
        Case("1,234", Option(123400)),
        Case("1 234", Option(123400)),
        Case(" 1 , 234 . 56 ", Option(123456)),
        Case("1.2.3", None),
        Case("", None),
        Case("--1", None),
        Case(".", None),
        Case(",", None),
        Case(".123", None),
        Case(",123", None)
      )

      for (testCase <- testCases) yield {
        testCase.test
        testCase.negated.test
        testCase.withPlusSignPrefix.test
      }
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
      ReferenceMoney(123456).formatFloat mustEqual "1,234.56"
      ReferenceMoney(-12345678).formatFloat mustEqual "-123,456.78"
      ReferenceMoney(-1234567890).formatFloat mustEqual "-12,345,678.90"
    }

    "withDate" in {
      val date = clock.now
      ReferenceMoney(-987).withDate(date) mustEqual DatedMoney(
        cents = -987,
        currency = Currency.default,
        date = date)
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
  }
}

package common.money

import java.time.Duration

import common.money.Currency.Gbp
import common.testing.FakeJsEntityAccess
import common.testing.TestModule
import hydro.common.time.Clock
import app.models.money.ExchangeRateMeasurement
import app.models.money.JsExchangeRateManager
import utest.TestSuite
import utest._

import scala.collection.immutable.Seq
import app.scala2js.Converters._

class MoneyTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val clock = testModule.fakeClock
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val exchangeRateManager: JsExchangeRateManager = testModule.exchangeRateManager

    "Money" - {
      "unary -" - {
        (-ReferenceMoney(8701)).cents ==> -8701
        (-ReferenceMoney(-8701)).cents ==> 8701
      }
      "centsToFloatString" - {
        Money.centsToFloatString(-8701) ==> "-87.01"
      }

      "floatToCents" - {
        Money.floatToCents(81.234) ==> 8123
        Money.floatToCents(-1.234) ==> -123
        Money.floatToCents(0.236) ==> 24
      }

      "floatStringToCents" - {
        case class Case(input: String, output: Option[Long]) {
          def negated: Case = Case("-" + input, output.map(cents => -cents))
          def withPlusSignPrefix: Case = Case("+" + input, output)

          def test() = {
            val resultTry = Money.tryFloatStringToCents(input)
            if (resultTry.toOption != output) {
              throw new java.lang.AssertionError(
                s"Money.tryFloatStringToCents('$input') = $resultTry, but expected $output")
            }
            resultTry.toOption ==> output
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
          testCase.test()
          testCase.negated.test()
          testCase.withPlusSignPrefix.test()
        }
      }
    }

    "ReferenceMoney" - {
      "+" - {
        ReferenceMoney(4) + ReferenceMoney(-5) ==> ReferenceMoney(-1)
      }

      "formatFloat" - {
        ReferenceMoney(0).formatFloat ==> "0.00"
        ReferenceMoney(87).formatFloat ==> "0.87"
        ReferenceMoney(987).formatFloat ==> "9.87"
        ReferenceMoney(-987).formatFloat ==> "-9.87"
        ReferenceMoney(-87).formatFloat ==> "-0.87"
        ReferenceMoney(-8701).formatFloat ==> "-87.01"
        ReferenceMoney(123456).formatFloat ==> "1,234.56"
        ReferenceMoney(-12345678).formatFloat ==> "-123,456.78"
        ReferenceMoney(-1234567890).formatFloat ==> "-12,345,678.90"
      }

      "withDate" - {
        val date = clock.now
        ReferenceMoney(-987)
          .withDate(date) ==> DatedMoney(cents = -987, currency = Currency.default, date = date)
      }
    }

    "MoneyWithGeneralCurrency" - {
      "numeric" - {
        val numeric = MoneyWithGeneralCurrency.numeric(Currency.Gbp)

        Seq(
          MoneyWithGeneralCurrency(-111, Currency.Gbp),
          MoneyWithGeneralCurrency(-222, Currency.Gbp)
        ).sum(numeric) ==> MoneyWithGeneralCurrency(-333, Currency.Gbp)

        try {
          Seq(
            MoneyWithGeneralCurrency(-111, Currency.Eur),
            MoneyWithGeneralCurrency(-222, Currency.Gbp)
          ).sum(numeric)
          throw new java.lang.AssertionError()
        } catch {
          case expected: IllegalArgumentException =>
        }
      }
    }

    "DatedMoney" - {
      "exchangedForReferenceCurrency" - {
        persistGbpMeasurement(daysBeforeNow = 1, ratio = 1.3)

        val money = DatedMoney(10, Currency.Gbp, clock.now)
        money.exchangedForReferenceCurrency ==> ReferenceMoney(13)
      }

      "exchangedForCurrency" - {
        persistGbpMeasurement(daysBeforeNow = 1, ratio = 1.3)

        val date = clock.now
        val money = DatedMoney(10, Currency.Gbp, date)
        money.exchangedForCurrency(Currency.default) ==> DatedMoney(13, Currency.default, date)
      }
    }
  }

  private def persistGbpMeasurement(daysBeforeNow: Int, ratio: Double)(
      implicit entityAccess: FakeJsEntityAccess,
      clock: Clock): Unit = {
    entityAccess.addWithRandomId(
      ExchangeRateMeasurement(
        date = clock.now.plus(Duration.ofDays(-daysBeforeNow)),
        foreignCurrencyCode = Gbp.code,
        ratioReferenceToForeignCurrency = ratio))
  }
}

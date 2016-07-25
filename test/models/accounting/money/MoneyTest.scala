package models.accounting.money

import common.Clock
import org.specs2.mutable._
import play.api.test.WithApplication
import play.twirl.api.Html

class MoneyTest extends Specification {

  "Money" should {
    "centsToFloatString" in new WithApplication {
      Money.centsToFloatString(-8701) mustEqual "-87.01"
    }

    "floatToCents" in new WithApplication {
      Money.floatToCents(81.234) mustEqual (8123)
      Money.floatToCents(-1.234) mustEqual (-123)
      Money.floatToCents(0.236) mustEqual (24)
    }
  }

  "ReferenceMoney" should {
    "+" in new WithApplication {
      ReferenceMoney(4) + ReferenceMoney(-5) mustEqual ReferenceMoney(-1)
    }

    "formatFloat" in new WithApplication {
      ReferenceMoney(0).formatFloat mustEqual "0.00"
      ReferenceMoney(87).formatFloat mustEqual "0.87"
      ReferenceMoney(987).formatFloat mustEqual "9.87"
      ReferenceMoney(-987).formatFloat mustEqual "-9.87"
      ReferenceMoney(-87).formatFloat mustEqual "-0.87"
      ReferenceMoney(-8701).formatFloat mustEqual "-87.01"
    }

    "toHtmlWithCurrency" in new WithApplication {
      ReferenceMoney(-987).toHtmlWithCurrency mustEqual Html("&euro; -9.87")
    }

    "withDate" in new WithApplication {
      val date = Clock.now
      ReferenceMoney(-987).withDate(date) mustEqual DatedMoney(cents = -987, currency = Currency.default, date = date)
    }
  }


  "MoneyWithGeneralCurrency" should {
    "numeric" in new WithApplication {
      val numeric = MoneyWithGeneralCurrency.numeric(Currency.Gbp)

      Seq(
        MoneyWithGeneralCurrency(-111, Currency.Gbp),
        MoneyWithGeneralCurrency(-222, Currency.Gbp)
      ).sum(numeric) mustEqual MoneyWithGeneralCurrency(-333, Currency.Gbp)

      try {
        Seq(
          MoneyWithGeneralCurrency(-111, Currency.Eur),
          MoneyWithGeneralCurrency(-222, Currency.Gbp)
        ).sum(numeric)
        throw new AssertionError("Expected IllegalArgumentException")
      } catch {
        case e: IllegalArgumentException => "Expected"
      }
    }

    "toHtmlWithCurrency" in new WithApplication {
      MoneyWithGeneralCurrency(-987, Currency.Gbp).toHtmlWithCurrency mustEqual Html("&pound; -9.87")
    }

  }

  "DatedMoney" should {
    "exchangedForReferenceCurrency" in new WithApplication {
      val money = DatedMoney(10, Currency.Gbp, Clock.now)
      money.exchangedForReferenceCurrency mustEqual ReferenceMoney(13)
    }

    "exchangedForCurrency" in new WithApplication {
      val date = Clock.now
      val money = DatedMoney(10, Currency.Gbp, date)
      money.exchangedForCurrency(Currency.default) mustEqual DatedMoney(13, Currency.default, date)
    }

    "toHtmlWithCurrency" in new WithApplication {
      DatedMoney(10, Currency.Gbp, Clock.now).toHtmlWithCurrency mustEqual
        Html("""&pound; 0.10 <span class="reference-currency">&euro; 0.13</span>""")
    }
  }
}

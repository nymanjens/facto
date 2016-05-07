package models.accounting

import java.util.{Date, Calendar}

import org.specs2.mutable._
import play.api.test.FakeApplication
import play.api.test.WithApplication

import common.testing.TestObjects._
import common.testing.TestUtils._

class MoneyTest extends Specification {

  "+" in new WithApplication {
    Money(4) + Money(-5) mustEqual Money(-1)
  }

  "formatFloat" in new WithApplication {
    Money(0).formatFloat mustEqual "0.00"
    Money(87).formatFloat mustEqual "0.87"
    Money(987).formatFloat mustEqual "9.87"
    Money(-987).formatFloat mustEqual "-9.87"
    Money(-87).formatFloat mustEqual "-0.87"
    Money(-8701).formatFloat mustEqual "-87.01"
  }
}
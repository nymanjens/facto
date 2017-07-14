package common

import java.time.Duration

import common.testing.TestModule
import common.time.LocalDateTime
import models.accounting.money.Currency.{Eur, Gbp, Usd}
import utest.{TestSuite, _}

import scala2js.Converters._

object JsI18nTest extends TestSuite {

  override def tests = TestSuite {

    "apply()" - {
      val jsI18n = new JsI18n(
        Map(
          "test.message" -> "This is a test message.",
          "test.message.with.args" -> "This is a test message with args {0} and {1}."
        ))

      jsI18n("test.message") ==> "This is a test message."
      jsI18n("test.message.with.args", "arg1", 2.2) ==> "This is a test message with args arg1 and 2.2."
    }
  }
}

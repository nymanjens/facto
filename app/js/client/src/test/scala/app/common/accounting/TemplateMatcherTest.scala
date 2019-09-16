package app.common.accounting

import java.time.Duration

import app.common.money.Currency.Gbp
import app.common.testing.TestModule
import app.common.testing.TestObjects
import app.common.testing.TestObjects._
import app.models.accounting.config.Template
import app.models.money.ExchangeRateMeasurement
import app.models.money.JsExchangeRateManager
import hydro.common.testing.FakeJsEntityAccess
import hydro.common.time.Clock
import utest._
import utest.TestSuite

import scala.collection.immutable.Seq

class TemplateMatcherTest extends TestSuite {

  override def tests = TestSuite {
    "getMatchingTemplate" - {
      "No templates" - {
        val templateMatcher = newTemplateMatcher()

        templateMatcher.getMatchingTemplate(Seq(testTransactionWithId)) ==> None
      }
      "With single transaction in template" - {
        val templateAbcd =
          createTemplate(transactions = Seq(createTemplateTransaction(description = "ABCD")))
        val templateEfgh =
          createTemplate(transactions = Seq(createTemplateTransaction(description = "EFGH")))
        val templateMatcher = newTemplateMatcher(templateAbcd, templateEfgh)

        "Matching template" - {
          templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "ABCD"))) ==>
            Some(templateAbcd)
        }
        "No matching template" - {
          templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "XYZ"))) ==> None
        }
      }
      "With multiple transactions in template" - {
        val templateAb =
          createTemplate(
            transactions = Seq(
              createTemplateTransaction(description = "AAA"),
              createTemplateTransaction(description = "BBB")))
        val templateCd =
          createTemplate(
            transactions = Seq(
              createTemplateTransaction(description = "CCC"),
              createTemplateTransaction(description = "DDD")))
        val templateMatcher = newTemplateMatcher(templateAb, templateCd)

        "Matching template" - {
          "Subset" - {
            templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "AAA"))) ==>
              Some(templateAb)
          }
          "Full set" - {
            templateMatcher.getMatchingTemplate(
              Seq(createTransaction(description = "AAA"), createTransaction(description = "BBB"))) ==>
              Some(templateAb)
          }
          "Full set out of order" - {
            templateMatcher.getMatchingTemplate(
              Seq(createTransaction(description = "BBB"), createTransaction(description = "AAA"))) ==>
              Some(templateAb)
          }
        }

        "No matching template" - {
          "Mismatch" - {
            templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "XYZ"))) ==> None
          }
          "Too many transactions" - {
            templateMatcher.getMatchingTemplate(
              Seq(
                createTransaction(description = "AAA"),
                createTransaction(description = "BBB"),
                createTransaction(description = "CCC"))) ==>
              Some(templateAb)
          }
        }
      }
    }
  }

  private def newTemplateMatcher(templates: Template*): TemplateMatcher = {
    val acountingConfig = testAccountingConfig.copy(templates = templates.toVector)
    val entityAccess = new TestModule().fakeEntityAccess
    new TemplateMatcher()(acountingConfig, entityAccess)
  }
}

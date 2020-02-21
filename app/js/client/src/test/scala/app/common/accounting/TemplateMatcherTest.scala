package app.common.accounting

import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.models.accounting.config.Template
import utest._
import utest.TestSuite

import scala.collection.immutable.Seq

object TemplateMatcherTest extends TestSuite {

  override def tests = TestSuite {
    "getMatchingTemplate" - {
      "No templates" - {
        val templateMatcher = newTemplateMatcher()

        templateMatcher.getMatchingTemplate(Seq(testTransactionWithId)) ==> None
      }
      "With single transaction in template" - {
        val templateA =
          createTemplate(transactions = Seq(createTemplateTransaction(description = "AAA")))
        val templateB =
          createTemplate(transactions = Seq(createTemplateTransaction(description = "BBB")))
        val templateMatcher = newTemplateMatcher(templateA, templateB)

        "Matching template" - {
          templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "AAA"))) ==>
            Some(templateA)
        }
        "No matching template" - {
          templateMatcher.getMatchingTemplate(Seq(createTransaction(description = "BBB"))) ==> None
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
      "Other properties" - {
        "category" - {
          val templateA =
            createTemplate(
              transactions =
                Seq(createTemplateTransaction(description = "XYZ", categoryCode = testCategoryA.code)))
          val templateB =
            createTemplate(
              transactions =
                Seq(createTemplateTransaction(description = "XYZ", categoryCode = testCategoryB.code)))
          val templateMatcher = newTemplateMatcher(templateA, templateB)

          "Matching template" - {
            templateMatcher.getMatchingTemplate(
              Seq(createTransaction(description = "XYZ", category = testCategoryB))) ==>
              Some(templateB)
          }
          "No matching template" - {
            templateMatcher.getMatchingTemplate(
              Seq(createTransaction(description = "XYZ", category = testCategoryC))) ==> None
          }
        }
        "beneficiary" - {
          "Without placeholder" - {
            val templateA =
              createTemplate(
                transactions =
                  Seq(createTemplateTransaction(description = "XYZ", beneficiaryCodeTpl = testAccountA.code)))
            val templateB =
              createTemplate(
                transactions =
                  Seq(createTemplateTransaction(description = "XYZ", beneficiaryCodeTpl = testAccountB.code)))
            val templateMatcher = newTemplateMatcher(templateA, templateB)

            "Matching template" - {
              templateMatcher.getMatchingTemplate(
                Seq(createTransaction(description = "XYZ", beneficiary = testAccountA))) ==>
                Some(templateB)
            }
            "No matching template" - {
              templateMatcher.getMatchingTemplate(
                Seq(createTransaction(description = "XYZ", beneficiary = testAccountC))) ==> None
            }
          }
          "With placeholder" - {
            val templateA =
              createTemplate(transactions = Seq(
                createTemplateTransaction(description = "XYZ", beneficiaryCodeTpl = "$" + "{account.code}")))
            val templateMatcher = newTemplateMatcher(templateA)

            "Matching template" - {
              templateMatcher.getMatchingTemplate(
                Seq(createTransaction(description = "XYZ", beneficiary = testAccountA))) ==>
                Some(templateA)
            }
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

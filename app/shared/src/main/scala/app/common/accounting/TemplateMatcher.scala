package app.common.accounting

import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import app.models.access.AppEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.accounting.Transaction
import hydro.common.GuavaReplacement.ImmutableSetMultimap

import scala.collection.immutable.Seq
import scala.collection.mutable

class TemplateMatcher(
    implicit accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    entityAccess: AppEntityAccess,
) {

  // **************** Private fields **************** //
  type InvolvedCategory = Option[String]
  type NumberOfTransactions = Int

  /**
    * Usage instructions:
    * - The number of transactions has to be exactly equal so use the right number there.
    * - The template categories are optional (per transaction). Try all categories in the transaction group to find all
    *   relevant templates and also try the 'None' value.
    **/
  private val templatesIndex: Map[NumberOfTransactions, ImmutableSetMultimap[InvolvedCategory, Template]] = {
    def toInvolvedCategoryMultimap(
        templates: Seq[Template]): ImmutableSetMultimap[InvolvedCategory, Template] = {
      val resultBuilder = ImmutableSetMultimap.builder[InvolvedCategory, Template]()

      for (template <- templates) {
        var addedToAnyCategory = false
        for {
          transactionTemplate <- template.transactions
        } {
          resultBuilder.put(Some(transactionTemplate.categoryCode), template)
          addedToAnyCategory = true
        }

        if (!addedToAnyCategory) {
          resultBuilder.put(None, template)
        }
      }

      resultBuilder.build()
    }

    accountingConfig.templates.groupBy(_.transactions.size).mapValues(toInvolvedCategoryMultimap)
  }

  // **************** Public API **************** //
  /**
    * Returns a template that a group with the given transactions is probably an instance of or None if no such
    * template exists.
    */
  def getMatchingTemplate(transactions: Seq[Transaction]): Option[Template] = {
    ???
  }

  // **************** Private helper methods **************** //
  private def matches(template: Template, transactions: Seq[Transaction]): Boolean = {
    def loginNameMatches =
      template.onlyShowForUserLoginNames.map { onlyShowForUserLoginNames =>
        if (onlyShowForUserLoginNames.nonEmpty) {
          onlyShowForUserLoginNames contains transactions.head.issuer.loginName
        } else {
          true
        }
      } getOrElse true

    def numberOfTransactionsMatch = template.transactions.size == transactions.size

    def transactionsMatch = (template.transactions zip transactions).forall {
      case (te, tr) => matches(te, tr)
    }

    def zeroSumMatches = if (template.zeroSum) isZeroSum(transactions) else true

    loginNameMatches && numberOfTransactionsMatch && transactionsMatch && zeroSumMatches
  }

  private def matches(templateTransaction: Template.Transaction, transaction: Transaction): Boolean = {
    // Notable absent fields: money reservoir and flow, because they tend to change more often

    val beneficiaryMatches =
      matchOrTrueIfPlaceholders(templateTransaction.beneficiaryCodeTpl, transaction.beneficiaryAccountCode)
    val categoryMatches = templateTransaction.categoryCode == transaction.category.code
    val descriptionMatches = transaction.description startsWith templateTransaction.descriptionTpl
    val detailDescriptionMatches = transaction.detailDescription startsWith templateTransaction.detailDescription
    val tagsMatch = templateTransaction.tags.forall(transaction.tags.contains)

    beneficiaryMatches && categoryMatches && descriptionMatches && detailDescriptionMatches && tagsMatch
  }

  private def isZeroSum(transactions: Seq[Transaction]): Boolean = {
    transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
  }

  private def matchOrTrueIfPlaceholders(templateValue: String, transactionValue: String): Boolean = {
    if (templateValue contains "$") {
      true
    } else {
      templateValue == transactionValue
    }
  }
}

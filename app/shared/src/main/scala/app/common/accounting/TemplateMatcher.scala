package app.common.accounting

import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import app.models.access.AppEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.accounting.Transaction

import scala.collection.immutable.Seq

class TemplateMatcher(
    implicit accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    entityAccess: AppEntityAccess,
) {

  // **************** Private fields **************** //
  type InvolvedCategories = Seq[String]

  private val templatesIndex: Map[InvolvedCategories, Seq[Template]] =
    accountingConfig.templates.groupBy(_.transactions.map(_.categoryCode)).withDefaultValue(Seq())

  // **************** Public API **************** //
  /**
    * Returns a template that a group with the given transactions is probably an instance of or None if no such
    * template exists.
    */
  def getMatchingTemplate(transactions: Seq[Transaction]): Option[Template] = {
    val involvedCategories = transactions.map(_.categoryCode)
    templatesIndex(involvedCategories).find(template => matches(template, transactions))
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

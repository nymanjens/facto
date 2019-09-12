package app.common.accounting

import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import app.models.access.AppEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.accounting.Transaction
import app.models.accounting.config.Category
import hydro.common.GuavaReplacement
import hydro.common.GuavaReplacement.ImmutableSetMultimap

import scala.collection.immutable.Seq
import scala.collection.mutable

class TemplateMatcher(
    implicit accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    entityAccess: AppEntityAccess,
) {

  // **************** Private fields **************** //
  private type Description = String

  /** Map from description to every template that involves that description. */
  private val templatesIndex: ImmutableSetMultimap[Description, Template] = {
    val mapBuilder = ImmutableSetMultimap.builder[Description, Template]()
    for {
      template <- accountingConfig.templates
      transaction <- template.transactions
    } {
      mapBuilder.put(transaction.description, template)
    }
    mapBuilder.build()
  }

  // **************** Public API **************** //
  /**
    * Returns a template that a group with the given subset of transactions is probably an instance of or None if no
    * such template exists.
    */
  def getMatchingTemplate(transactionsSubset: Seq[Transaction]): Option[Template] = {
    // Only try first description, since the template we are looking for *has* to be at least in that entry
    templatesIndex
      .get(transactionsSubset.head.description)
      .find(template => matches(template, transactionsSubset))
  }

  // **************** Private helper methods **************** //
  private def matches(template: Template, transactionsSubset: Seq[Transaction]): Boolean = {
    // Note: Because of an earlier bug where transaction order was not preserved when added via the external API
    // (by template), we ignore the transaction order.

    def tooManyTransactions = transactionsSubset.size > template.transactions.size

    def loginNameMatches =
      template.onlyShowForUserLoginNames.map { onlyShowForUserLoginNames =>
        if (onlyShowForUserLoginNames.nonEmpty) {
          onlyShowForUserLoginNames contains transactionsSubset.head.issuer.loginName
        } else {
          true
        }
      } getOrElse true

    def transactionsMatch = template.transactions.permutations.exists { permutation =>
      (permutation zip transactionsSubset).forall {
        case (te, tr) => matches(te, tr)
      }
    }

    !tooManyTransactions && loginNameMatches && transactionsMatch
  }

  private def matches(templateTransaction: Template.Transaction, transaction: Transaction): Boolean = {
    // Notable absent fields: money reservoir and flow, because they tend to change more often

    def descriptionMatches = transaction.description == templateTransaction.description
    def beneficiaryMatches =
      matchOrTrueIfPlaceholders(templateTransaction.beneficiaryCodeTpl, transaction.beneficiaryAccountCode)
    def categoryMatches = templateTransaction.categoryCode == transaction.category.code
    def tagsMatch = templateTransaction.tags.forall(transaction.tags.contains)

    descriptionMatches && beneficiaryMatches && categoryMatches && tagsMatch
  }

  private def matchOrTrueIfPlaceholders(templateValue: String, transactionValue: String): Boolean = {
    if (templateValue contains "$") {
      true
    } else {
      templateValue == transactionValue
    }
  }
}

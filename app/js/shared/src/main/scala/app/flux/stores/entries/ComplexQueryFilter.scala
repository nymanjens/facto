package app.flux.stores.entries

import common.GuavaReplacement.Splitter
import common.ScalaUtils.visibleForTesting
import common.money.Money
import app.flux.stores.entries.ComplexQueryFilter.Prefix
import app.flux.stores.entries.ComplexQueryFilter.QueryFilterPair
import app.flux.stores.entries.ComplexQueryFilter.QueryPart
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.accounting._
import app.models.accounting.config.Config

import scala.collection.immutable.Seq
import scala.collection.mutable
import hydro.scala2js.StandardConverters._

private[stores] final class ComplexQueryFilter(implicit entityAccess: JsEntityAccess,
                                               accountingConfig: Config) {

  // **************** Public API **************** //
  def fromQuery(query: String): DbQuery.Filter[Transaction] = {
    if (query.trim.isEmpty) {
      DbQuery.Filter.NullFilter()
    } else {
      DbQuery.Filter.And(
        Seq(
          splitInParts(query)
            .map {
              case QueryPart(string, negated) =>
                val filterPair = createFilterPair(singlePartWithoutNegation = string)

                if (negated) {
                  filterPair.negated
                } else {
                  filterPair
                }
            }
            .sortBy(_.estimatedExecutionCost)
            .map(_.positiveFilter): _*
        ))
    }
  }

  // **************** Private helper methods **************** //
  private def createFilterPair(singlePartWithoutNegation: String): QueryFilterPair = {
    def filterOptions[T](inputString: String, options: Seq[T])(nameFunc: T => String): Seq[T] =
      options.filter(option => nameFunc(option).toLowerCase contains inputString.toLowerCase)
    def fallback =
      QueryFilterPair.containsIgnoreCase(ModelField.Transaction.description, singlePartWithoutNegation)

    parsePrefixAndSuffix(singlePartWithoutNegation) match {
      case Some((prefix, suffix)) =>
        prefix match {
          case Prefix.Issuer =>
            QueryFilterPair.anyOf(
              ModelField.Transaction.issuerId,
              filterOptions(suffix, entityAccess.newQuerySyncForUser().data())(_.name).map(_.id))
          case Prefix.Beneficiary =>
            QueryFilterPair.anyOf(
              ModelField.Transaction.beneficiaryAccountCode,
              filterOptions(suffix, accountingConfig.accountsSeq)(_.longName).map(_.code))
          case Prefix.Reservoir =>
            QueryFilterPair.anyOf(
              ModelField.Transaction.moneyReservoirCode,
              filterOptions(suffix, accountingConfig.moneyReservoirs(includeHidden = true))(_.name)
                .map(_.code))
          case Prefix.Category =>
            QueryFilterPair.anyOf(
              ModelField.Transaction.categoryCode,
              filterOptions(suffix, accountingConfig.categoriesSeq)(_.name).map(_.code))
          case Prefix.Description =>
            QueryFilterPair.containsIgnoreCase(ModelField.Transaction.description, suffix)
          case Prefix.Flow =>
            Money.floatStringToCents(suffix).map { flowInCents =>
              QueryFilterPair.isEqualTo(ModelField.Transaction.flowInCents, flowInCents)
            } getOrElse fallback
          case Prefix.Detail =>
            QueryFilterPair.containsIgnoreCase(ModelField.Transaction.detailDescription, suffix)
          case Prefix.Tag =>
            QueryFilterPair.seqContains(ModelField.Transaction.tags, suffix)
        }
      case None => fallback
    }
  }

  @visibleForTesting private[stores] def parsePrefixAndSuffix(string: String): Option[(Prefix, String)] = {
    val prefixStringToPrefix: Map[String, Prefix] = {
      for {
        prefix <- Prefix.all
        prefixString <- prefix.prefixStrings
      } yield prefixString -> prefix
    }.toMap

    val split = Splitter.on(':').split(string).toList
    split match {
      case prefix :: suffix if (prefixStringToPrefix contains prefix) && suffix.mkString(":").nonEmpty =>
        Some((prefixStringToPrefix(prefix), suffix.mkString(":")))
      case _ => None
    }
  }
  @visibleForTesting private[stores] def splitInParts(query: String): Seq[QueryPart] = {
    val quotes = Seq('"', '\'')
    val parts = mutable.Buffer[QueryPart]()
    val nextPart = new StringBuilder
    var currentQuote: Option[Char] = None
    var negated = false

    for (char <- query) char match {
      case '-' if nextPart.isEmpty && currentQuote.isEmpty && !negated =>
        negated = true
      case _
          if (quotes contains char) && (nextPart.isEmpty || nextPart
            .endsWith(":")) && currentQuote.isEmpty =>
        currentQuote = Some(char)
      case _ if currentQuote contains char =>
        currentQuote = None
      case ' ' if currentQuote.isEmpty && nextPart.nonEmpty =>
        parts += QueryPart(nextPart.result().trim, negated = negated)
        nextPart.clear()
        negated = false
      case ' ' if currentQuote.isEmpty && nextPart.isEmpty =>
      // do nothing
      case _ =>
        nextPart += char
    }
    if (nextPart.nonEmpty) {
      parts += QueryPart(nextPart.result().trim, negated = negated)
    }
    Seq(parts: _*)
  }
}

object ComplexQueryFilter {
  private case class QueryFilterPair(positiveFilter: DbQuery.Filter[Transaction],
                                     negativeFilter: DbQuery.Filter[Transaction],
                                     estimatedExecutionCost: Int) {
    def negated: QueryFilterPair =
      QueryFilterPair(
        positiveFilter = negativeFilter,
        negativeFilter = positiveFilter,
        estimatedExecutionCost = estimatedExecutionCost)
  }

  private object QueryFilterPair {
    def isEqualTo[V](field: ModelField[V, Transaction], value: V): QueryFilterPair =
      anyOf(field, Seq(value))

    def anyOf[V](field: ModelField[V, Transaction], values: Seq[V]): QueryFilterPair =
      values match {
        case Seq(value) =>
          QueryFilterPair(
            estimatedExecutionCost = 1,
            positiveFilter = field === value,
            negativeFilter = field !== value)
        case _ =>
          QueryFilterPair(
            estimatedExecutionCost = 2,
            positiveFilter = field isAnyOf values,
            negativeFilter = field isNoneOf values)
      }

    def containsIgnoreCase(field: ModelField[String, Transaction], substring: String): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field containsIgnoreCase substring,
        negativeFilter = field doesntContainIgnoreCase substring)

    def seqContains(field: ModelField[Seq[String], Transaction], value: String): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field contains value,
        negativeFilter = field doesntContain value
      )
  }

  @visibleForTesting private[stores] case class QueryPart(unquotedString: String, negated: Boolean = false)
  @visibleForTesting private[stores] object QueryPart {
    def not(unquotedString: String): QueryPart = QueryPart(unquotedString, negated = true)
  }

  @visibleForTesting private[stores] sealed abstract class Prefix private (val prefixStrings: Seq[String]) {
    override def toString = getClass.getSimpleName
  }
  @visibleForTesting private[stores] object Prefix {
    def all: Seq[Prefix] =
      Seq(Issuer, Beneficiary, Reservoir, Category, Description, Flow, Detail, Tag)

    object Issuer extends Prefix(Seq("issuer", "i", "u", "user"))
    object Beneficiary extends Prefix(Seq("beneficiary", "b"))
    object Reservoir extends Prefix(Seq("reservoir", "r"))
    object Category extends Prefix(Seq("category", "c"))
    object Description extends Prefix(Seq("description"))
    object Flow extends Prefix(Seq("flow", "amount", "a"))
    object Detail extends Prefix(Seq("detail"))
    object Tag extends Prefix(Seq("tag", "t"))
  }
}

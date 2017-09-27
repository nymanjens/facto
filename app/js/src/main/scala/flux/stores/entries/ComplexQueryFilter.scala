package flux.stores.entries

import common.GuavaReplacement.Splitter
import common.ScalaUtils.visibleForTesting
import flux.stores.entries.ComplexQueryFilter.{CombinedQueryFilter, Prefix, QueryFilter, QueryPart}
import jsfacades.LokiJs
import jsfacades.LokiJs.ResultSet
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money.Money

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala2js.Converters._
import scala2js.{Keys, Scala2Js}

private[stores] final class ComplexQueryFilter(implicit userManager: User.Manager, accountingConfig: Config) {

  // **************** Public API **************** //
  def fromQuery(query: String): ResultSet[Transaction] => ResultSet[Transaction] = parseQuery(query).apply

  // **************** Private helper methods **************** //
  private def parseQuery(query: String): CombinedQueryFilter = {
    CombinedQueryFilter {
      splitInParts(query) map {
        case QueryPart(string, negated) =>
          val positiveFilter = createPositiveFilter(singlePartWithoutNegation = string)

          if (negated) {
            QueryFilter.not(positiveFilter)
          } else {
            positiveFilter
          }
      }
    }
  }

  private def createPositiveFilter(singlePartWithoutNegation: String): QueryFilter = {
    trait FilterCreator[T] {
      def createFieldFilter[V: Scala2Js.Converter](key: Scala2Js.Key[V, Transaction])(
          keyFunc: T => V): QueryFilter
    }
    def filterOptions[T](inputString: String, options: Seq[T])(nameFunc: T => String): Seq[T] =
      options.filter(option => nameFunc(option).toLowerCase contains inputString.toLowerCase)

    parsePrefixAndSuffix(singlePartWithoutNegation) match {
      case Some((prefix, suffix)) =>
        prefix match {
          case Prefix.Issuer =>
            QueryFilter.anyOf(
              Keys.Transaction.issuerId,
              filterOptions(suffix, userManager.fetchAll())(_.name).map(_.id))
          case Prefix.Beneficiary =>
            QueryFilter.anyOf(
              Keys.Transaction.beneficiaryAccountCode,
              filterOptions(suffix, accountingConfig.accountsSeq)(_.longName).map(_.code))
          case Prefix.Reservoir =>
            QueryFilter.anyOf(
              Keys.Transaction.moneyReservoirCode,
              filterOptions(suffix, accountingConfig.moneyReservoirs(includeHidden = true))(_.name)
                .map(_.code))
          case Prefix.Category =>
            QueryFilter.anyOf(
              Keys.Transaction.categoryCode,
              filterOptions(suffix, accountingConfig.categoriesSeq)(_.name).map(_.code))
          case Prefix.Description =>
            QueryFilter.containsIgnoreCase(Keys.Transaction.description, suffix)
          case Prefix.Flow =>
            Money.floatStringToCents(suffix).map { flowInCents =>
              QueryFilter.isEqualTo(Keys.Transaction.flowInCents, flowInCents)
            } getOrElse QueryFilter.nullFilter
          case Prefix.Detail =>
            QueryFilter.containsIgnoreCase(Keys.Transaction.detailDescription, suffix)
          case Prefix.Tag =>
            QueryFilter.seqContains(Keys.Transaction.tags, suffix)
        }
      case None =>
        QueryFilter.containsIgnoreCase(Keys.Transaction.description, singlePartWithoutNegation)
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
  private trait QueryFilter {
    def apply(resultSet: LokiJs.ResultSet[Transaction], invert: Boolean = false): ResultSet[Transaction]
    def estimatedExecutionCost: Int
  }

  private object QueryFilter {
    val nullFilter: QueryFilter =
      from(estimatedExecutionCost = 0, positiveApply = r => r, negativeApply = r => r)

    private def from(
        estimatedExecutionCost: Int,
        positiveApply: LokiJs.ResultSet[Transaction] => LokiJs.ResultSet[Transaction],
        negativeApply: LokiJs.ResultSet[Transaction] => LokiJs.ResultSet[Transaction]): QueryFilter =
      new QueryFilter {
        val estimatedExecutionCostArg: Int = estimatedExecutionCost
        override def apply(resultSet: ResultSet[Transaction], invert: Boolean) = {
          if (invert) {
            negativeApply(resultSet)
          } else {
            positiveApply(resultSet)
          }
        }
        override def estimatedExecutionCost = estimatedExecutionCostArg
      }

    def not(delegate: QueryFilter): QueryFilter = new QueryFilter {
      override def apply(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegate(resultSet, invert = !invert)
      override def estimatedExecutionCost = delegate.estimatedExecutionCost
    }

    def isEqualTo[V: Scala2Js.Converter](key: Scala2Js.Key[V, Transaction], value: V): QueryFilter =
      anyOf(key, Seq(value))

    def anyOf[V: Scala2Js.Converter](key: Scala2Js.Key[V, Transaction], values: Seq[V]): QueryFilter =
      values match {
        case Seq() => nullFilter
        case Seq(value) =>
          from(
            estimatedExecutionCost = 1,
            positiveApply = _.filter(key, value),
            negativeApply = _.filterNot(key, value))
        case _ =>
          from(
            estimatedExecutionCost = 2,
            positiveApply = _.filterAnyOf(key, values),
            negativeApply = _.filterNoneOf(key, values))
      }

    def containsIgnoreCase(key: Scala2Js.Key[String, Transaction], substring: String): QueryFilter =
      from(
        estimatedExecutionCost = 3,
        positiveApply = _.filterContainsIgnoreCase(key, substring),
        negativeApply = _.filterDoesntContainIgnoreCase(key, substring))

    def seqContains(key: Scala2Js.Key[Seq[String], Transaction], value: String): QueryFilter =
      from(
        estimatedExecutionCost = 3,
        positiveApply = _.filterSeqContains(key, value),
        negativeApply = _.filterSeqDoesntContain(key, value))
  }

  private case class CombinedQueryFilter(delegates: Seq[QueryFilter]) {
    def apply(resultSet: ResultSet[Transaction]): ResultSet[Transaction] = {
      var newResultSet = resultSet
      for (filter <- delegates.sortBy(_.estimatedExecutionCost)) {
        newResultSet = filter(newResultSet)
      }
      newResultSet
    }
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

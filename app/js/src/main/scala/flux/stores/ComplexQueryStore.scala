package flux.stores

import common.GuavaReplacement.Splitter
import common.ScalaUtils.visibleForTesting
import flux.stores.ComplexQueryStore.{CombinedQueryFilter, Prefix, QueryFilter, QueryPart}
import jsfacades.LokiJs
import jsfacades.LokiJs.ResultSet
import models.access.RemoteDatabaseProxy
import models.accounting._

import scala.collection.immutable.Seq
import scala.collection.mutable

final class ComplexQueryStore(implicit database: RemoteDatabaseProxy) {

  // **************** Public API **************** //
  def getMatchingTransactions(query: String): Seq[Transaction] = {
    ???
  }

  // **************** Private helper methods **************** //
  private def parseQuery(query: String): CombinedQueryFilter = {
    CombinedQueryFilter {
      splitInParts(query) map {
        case QueryPart(string, negated) =>
          val prefixAndSuffix = parsePrefixAndSuffix(string)

          string match {
            case _ => ???
          }
      }
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
      case _ if (quotes contains char) && nextPart.isEmpty && currentQuote.isEmpty =>
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

  // **************** Private inner classes **************** //
//  private case class Query(descriptionSubstrings: Seq[Query.Negatable[String]],
//                           reservoir: Seq[Query.Negatable[MoneyReservoir]],
//                           category: Seq[Query.Negatable[Category]])
//  private object Query {
//    private case class Negatable[T](query: T, isNegated: Boolean)
//  }
}

object ComplexQueryStore {
  private trait QueryFilter {
    def applyWithOperator(resultSet: LokiJs.ResultSet[Transaction], invert: Boolean = false): Unit = {}
    def applyWithFunction(resultSet: LokiJs.ResultSet[Transaction], invert: Boolean = false): Unit = {}
  }

  private case class CombinedQueryFilter(delegates: Seq[QueryFilter]) {
    def apply(resultSet: ResultSet[Transaction]): Unit = {
      delegates.foreach(_.applyWithOperator(resultSet))
      delegates.foreach(_.applyWithFunction(resultSet))
    }
  }

  private object QueryFilter {
    case class Negated(delegate: QueryFilter) extends QueryFilter {
      override def applyWithOperator(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegate.applyWithOperator(resultSet, invert = !invert)
      override def applyWithFunction(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegate.applyWithFunction(resultSet, invert = !invert)
    }
  }

  @visibleForTesting private[stores] case class QueryPart(unquotedString: String, negated: Boolean = false)
  @visibleForTesting private[stores] object QueryPart {
    def not(unquotedString: String): QueryPart = QueryPart(unquotedString, negated = true)
  }

  @visibleForTesting private[stores] sealed case class Prefix(prefixStrings: Seq[String]) {
    override def toString = getClass.getSimpleName
  }
  @visibleForTesting private[stores] object Prefix {
    def all: Seq[Prefix] =
      Seq(Issuer, Beneficiary, Reservoir, Category, Description, Flow, Detail, Tag, Date)

    object Issuer extends Prefix(Seq("issuer", "i"))
    object Beneficiary extends Prefix(Seq("beneficiary", "b"))
    object Reservoir extends Prefix(Seq("reservoir", "r"))
    object Category extends Prefix(Seq("category", "c"))
    object Description extends Prefix(Seq("description"))
    object Flow extends Prefix(Seq("flow", "amount", "a"))
    object Detail extends Prefix(Seq("detail"))
    object Tag extends Prefix(Seq("tag", "t"))
    object Date extends Prefix(Seq("date", "d"))
  }
}

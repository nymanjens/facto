package flux.stores

import common.ScalaUtils.visibleForTesting
import flux.stores.ComplexQueryStore.{QueryFilter, QueryPart}
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
  private def parseQuery(query: String): QueryFilter = {
    ???
//    QueryFilter.Combined {
//      splitInParts(query) map ???
//    }
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

  private object QueryFilter {
    case class Negated(delegate: QueryFilter) extends QueryFilter {
      override def applyWithOperator(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegate.applyWithOperator(resultSet, invert = !invert)
      override def applyWithFunction(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegate.applyWithFunction(resultSet, invert = !invert)
    }
    case class Combined(delegates: Seq[QueryFilter]) extends QueryFilter {
      override def applyWithOperator(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegates.foreach(_.applyWithOperator(resultSet, invert = invert))
      override def applyWithFunction(resultSet: ResultSet[Transaction], invert: Boolean) =
        delegates.foreach(_.applyWithFunction(resultSet, invert = invert))
    }
  }

  @visibleForTesting private[stores] case class QueryPart(unquotedString: String, negated: Boolean = false)
  @visibleForTesting private[stores] object QueryPart {
    def not(unquotedString: String): QueryPart = QueryPart(unquotedString, negated = true)
  }
}

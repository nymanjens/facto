package app.common.accounting

import app.common.accounting.ComplexQueryFilter.Prefix
import app.common.accounting.ComplexQueryFilter.QueryFilterPair
import app.common.accounting.ComplexQueryFilter.QueryPart
import app.common.money.Money
import app.common.TagFiltering
import app.models.access.AppEntityAccess
import app.models.access.ModelFields
import app.models.accounting._
import app.models.accounting.config.Config
import hydro.common.Annotations.visibleForTesting
import hydro.common.GuavaReplacement.Splitter
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQuery
import hydro.models.access.DbQuery.PicklableOrdering
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.ModelField

import java.time.Month
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.Try

final class ComplexQueryFilter(implicit
    entityAccess: AppEntityAccess,
    accountingConfig: Config,
) {

  // **************** Public API **************** //
  def fromQuery(query: String): DbQuery.Filter[Transaction] = {
    if (query.trim.isEmpty) {
      DbQuery.Filter.NullFilter()
    } else {
      DbQuery.Filter.And(
        Seq(
          splitInParts(query)
            .map(toFilterPair)
            .sortBy(_.estimatedExecutionCost)
            .map(_.positiveFilter): _*
        )
      )
    }
  }

  // **************** Private helper methods **************** //
  private def toFilterPair(queryPart: QueryPart): QueryFilterPair = {
    val QueryPart(content, negated) = queryPart

    val filterPair = content match {
      case QueryPart.Content.Literal(content) => createFilterPair(singlePartWithoutNegation = content)
    }

    if (negated) {
      filterPair.negated
    } else {
      filterPair
    }
  }

  private def createFilterPair(singlePartWithoutNegation: String): QueryFilterPair = {
    def filterOptions[T](inputString: String, options: Seq[T])(nameFunc: T => String): Seq[T] =
      options.filter(option => nameFunc(option).toLowerCase contains inputString.toLowerCase)
    def fallback = {
      QueryFilterPair.or(
        QueryFilterPair.containsIgnoreCase(ModelFields.Transaction.description, singlePartWithoutNegation),
        QueryFilterPair.seqContains(ModelFields.Transaction.tags, singlePartWithoutNegation),
        QueryFilterPair
          .containsIgnoreCase(ModelFields.Transaction.detailDescription, singlePartWithoutNegation),
      )
    }

    parsePrefixAndSuffix(singlePartWithoutNegation) match {
      case Some((prefix, suffix)) =>
        prefix match {
          case Prefix.Issuer =>
            QueryFilterPair.anyOf(
              ModelFields.Transaction.issuerId,
              filterOptions(suffix, entityAccess.newQuerySyncForUser().data())(_.name).map(_.id),
            )
          case Prefix.Beneficiary =>
            QueryFilterPair.anyOf(
              ModelFields.Transaction.beneficiaryAccountCode,
              filterOptions(suffix, accountingConfig.accountsSeq)(_.longName).map(_.code),
            )
          case Prefix.Reservoir =>
            QueryFilterPair.anyOf(
              ModelFields.Transaction.moneyReservoirCode,
              filterOptions(suffix, accountingConfig.moneyReservoirs(includeHidden = true))(_.name)
                .map(_.code),
            )
          case Prefix.Category =>
            QueryFilterPair.anyOf(
              ModelFields.Transaction.categoryCode,
              // Adding suffix as categoryCode to allow filtering on dummy categories such as "Exchange"
              filterOptions(suffix, accountingConfig.categoriesSeq)(_.name).map(_.code) :+ suffix,
            )
          case Prefix.Description =>
            QueryFilterPair.containsIgnoreCase(ModelFields.Transaction.description, suffix)
          case Prefix.Flow =>
            Money.floatStringToCents(suffix).map { flowInCents =>
              QueryFilterPair.or(
                QueryFilterPair.isEqualTo(ModelFields.Transaction.flowInCents, flowInCents),
                // Include a check for the inverse flow
                QueryFilterPair.isEqualTo(ModelFields.Transaction.flowInCents, -flowInCents),
              )
            } getOrElse fallback
          case Prefix.FlowMinimum =>
            Money.floatStringToCents(suffix).map { flowInCents =>
              QueryFilterPair.isGreaterOrEqualThan(ModelFields.Transaction.flowInCents, flowInCents)
            } getOrElse fallback
          case Prefix.FlowMaximum =>
            Money.floatStringToCents(suffix).map { flowInCents =>
              QueryFilterPair.isLessOrEqualThan(ModelFields.Transaction.flowInCents, flowInCents)
            } getOrElse fallback
          case Prefix.Detail =>
            QueryFilterPair.containsIgnoreCase(ModelFields.Transaction.detailDescription, suffix)
          case Prefix.Tag =>
            QueryFilterPair.seqContains(
              ModelFields.Transaction.tagsNormalized,
              TagFiltering.normalize(suffix),
            )
          case Prefix.ConsumedStartYear =>
            Try(suffix.toInt).toOption.map { year =>
              QueryFilterPair.isGreaterOrEqualThan(
                ModelFields.Transaction.consumedDate,
                LocalDateTime.of(year, Month.JANUARY, dayOfMonth = 1, hour = 0, minute = 0),
              )
            } getOrElse fallback
        }
      case None => fallback
    }
  }

  @visibleForTesting private[accounting] def parsePrefixAndSuffix(
      string: String
  ): Option[(Prefix, String)] = {
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
  @visibleForTesting private[accounting] def splitInParts(query: String): Seq[QueryPart] = {
    val quotes = Seq('"', '\'')
    val parts = mutable.Buffer[QueryPart]()
    val nextPart = new StringBuilder
    var currentQuote: Option[Char] = None
    var negated = false

    for (char <- query) {
      currentQuote match {
        case None =>
          char match {
            case '-' if nextPart.isEmpty && !negated =>
              negated = true
            case _
                if (quotes contains char) && (nextPart.isEmpty || nextPart
                  .endsWith(":")) =>
              currentQuote = Some(char)
            case ' ' if nextPart.nonEmpty =>
              parts += QueryPart(QueryPart.Content.Literal(nextPart.result().trim), negated = negated)
              nextPart.clear()
              negated = false
            case ' ' if nextPart.isEmpty =>
              // do nothing
            case _ =>
              nextPart += char
          }
        case Some(quoteCurrentlyIn) =>
          char match {
            case `quoteCurrentlyIn` =>
              currentQuote = None
            case _ =>
              nextPart += char
          }
      }
    }
    if (nextPart.nonEmpty) {
      parts += QueryPart(QueryPart.Content.Literal(nextPart.result().trim), negated = negated)
    }
    Seq(parts: _*)
  }
}

object ComplexQueryFilter {
  private case class QueryFilterPair(
      positiveFilter: DbQuery.Filter[Transaction],
      negativeFilter: DbQuery.Filter[Transaction],
      estimatedExecutionCost: Int,
  ) {
    def negated: QueryFilterPair = {
      QueryFilterPair(
        positiveFilter = negativeFilter,
        negativeFilter = positiveFilter,
        estimatedExecutionCost = estimatedExecutionCost,
      )
    }
  }

  private object QueryFilterPair {

    def or(queryFilterPairs: QueryFilterPair*): QueryFilterPair = {
      QueryFilterPair(
        positiveFilter = DbQuery.Filter.Or(queryFilterPairs.toVector.map(_.positiveFilter)),
        negativeFilter = DbQuery.Filter.And(queryFilterPairs.toVector.map(_.negativeFilter)),
        estimatedExecutionCost = queryFilterPairs.map(_.estimatedExecutionCost).sum,
      )
    }

    def isEqualTo[V](field: ModelField[V, Transaction], value: V): QueryFilterPair =
      anyOf(field, Seq(value))

    def isGreaterOrEqualThan[V: PicklableOrdering](
        field: ModelField[V, Transaction],
        value: V,
    ): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 1,
        positiveFilter = field >= value,
        negativeFilter = field < value,
      )

    def isLessOrEqualThan[V: PicklableOrdering](
        field: ModelField[V, Transaction],
        value: V,
    ): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 1,
        positiveFilter = field <= value,
        negativeFilter = field > value,
      )

    def anyOf[V](field: ModelField[V, Transaction], values: Seq[V]): QueryFilterPair =
      values match {
        case Seq(value) =>
          QueryFilterPair(
            estimatedExecutionCost = 1,
            positiveFilter = field === value,
            negativeFilter = field !== value,
          )
        case _ =>
          QueryFilterPair(
            estimatedExecutionCost = 2,
            positiveFilter = field isAnyOf values,
            negativeFilter = field isNoneOf values,
          )
      }

    def containsIgnoreCase(field: ModelField[String, Transaction], substring: String): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field containsIgnoreCase substring,
        negativeFilter = field doesntContainIgnoreCase substring,
      )

    def seqContains(field: ModelField[Seq[String], Transaction], value: String): QueryFilterPair =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field contains value,
        negativeFilter = field doesntContain value,
      )
  }

  @visibleForTesting private[accounting] case class QueryPart(
      content: QueryPart.Content,
      negated: Boolean = false,
  )
  @visibleForTesting private[accounting] object QueryPart {
    def not(content: Content): QueryPart = QueryPart(content, negated = true)

    sealed trait Content
    object Content {
      case class Literal(content: String) extends Content
      case class Or(queryParts: Seq[QueryPart]) extends Content
    }
  }

  @visibleForTesting private[accounting] sealed abstract class Prefix private (
      val prefixStrings: Seq[String]
  ) {
    override def toString = getClass.getSimpleName
  }
  @visibleForTesting private[accounting] object Prefix {
    def all: Seq[Prefix] =
      Seq(
        Issuer,
        Beneficiary,
        Reservoir,
        Category,
        Description,
        Flow,
        FlowMinimum,
        FlowMaximum,
        Detail,
        Tag,
        ConsumedStartYear,
      )

    object Issuer extends Prefix(Seq("issuer", "i", "u", "user"))
    object Beneficiary extends Prefix(Seq("beneficiary", "b"))
    object Reservoir extends Prefix(Seq("reservoir", "r"))
    object Category extends Prefix(Seq("category", "c"))
    object Description extends Prefix(Seq("description"))
    object Flow extends Prefix(Seq("flow", "amount", "a"))
    object FlowMinimum extends Prefix(Seq("minFlow", "minAmount", "minA"))
    object FlowMaximum extends Prefix(Seq("maxFlow", "maxAmount", "maxA"))
    object Detail extends Prefix(Seq("detail"))
    object Tag extends Prefix(Seq("tag", "t"))
    object ConsumedStartYear extends Prefix(Seq("consumedStart", "start"))
  }
}

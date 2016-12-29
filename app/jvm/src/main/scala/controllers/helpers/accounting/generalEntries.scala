package controllers.helpers.accounting

import com.google.inject.{Inject, Singleton}
import collection.immutable.Seq
import scala.collection.JavaConverters._
import com.google.common.base.{Joiner, Splitter}
import common.time.LocalDateTime

import com.google.common.hash.Hashing
import common.time.JavaTimeImplicits._
import models.SlickUtils.dbApi._
import models.SlickUtils.{localDateTimeToSqlDateMapper, dbRun}
import models.accounting.{Transaction, SlickTransactionManager}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import controllers.helpers.ControllerHelperCache
import controllers.helpers.ControllerHelperCache.CacheIdentifier

case class GeneralEntry(override val transactions: Seq[Transaction])
  extends GroupedTransactions(transactions)

@Singleton()
final class GeneralEntries @Inject()(implicit accountingConfig: Config,
                                     transactionManager: SlickTransactionManager) {

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(n: Int): Seq[GeneralEntry] = {
    val transactions: Seq[Transaction] =
      dbRun(
        transactionManager.newQuery
          .sortBy(r => (r.transactionDate.desc, r.createdDate.desc))
          .take(3 * n))
        .reverse
        .toList

    var entries = transactions.map(t => GeneralEntry(Seq(t)))

    entries = combineConsecutiveOfSameGroup(entries)

    entries.takeRight(n)
  }

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEndowments(account: Account, n: Int): Seq[GeneralEntry] =
    ControllerHelperCache.cached(FetchLastNEndowments(account, n)) {
      val transactions: Seq[Transaction] =
        dbRun(
          transactionManager.newQuery
            .filter(_.categoryCode === accountingConfig.constants.endowmentCategory.code)
            .filter(_.beneficiaryAccountCode === account.code)
            .sortBy(r => (r.consumedDate.desc, r.createdDate.desc))
            .take(3 * n))
          .reverse
          .toList

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = combineConsecutiveOfSameGroup(entries)

      entries.takeRight(n)
    }

  /* Returns all entries that contain the given query, with the most relevant entries first. */
  def search(query: String): Seq[GeneralEntry] = {
    val transactions: Seq[Transaction] =
      dbRun(
        transactionManager.newQuery
          .sortBy(r => (r.createdDate, r.transactionDate)))
        .toList

    val filteredTransactions: Seq[Transaction] = {
      val queryParts: Seq[String] = Splitter.on(" ")
        .omitEmptyStrings()
        .trimResults()
        .splitToList(query)
        .asScala
        .toVector
      val scoreMap = transactions
        .map { t => (t, QueryScore(t, queryParts)) }
        .toMap
      transactions
        .filter(t => scoreMap(t).matchesQuery)
        .sortBy(t => scoreMap(t))
        .reverse
    }

    val entries = filteredTransactions.map(t => GeneralEntry(Seq(t)))
    combineConsecutiveOfSameGroup(entries)
  }

  private[accounting] def combineConsecutiveOfSameGroup(entries: Seq[GeneralEntry]): Seq[GeneralEntry] = {
    GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
      /* combine */ (first, last) => GeneralEntry(first.transactions ++ last.transactions)
    }
  }

  private case class FetchLastNEndowments(account: Account, n: Int) extends CacheIdentifier[Seq[GeneralEntry]] {
    protected override def invalidateWhenUpdating = {
      case transaction: Transaction =>
        transaction.categoryCode == accountingConfig.constants.endowmentCategory.code &&
          transaction.beneficiaryAccountCode == account.code
    }
  }
}

private case class QueryScore(scoreNumber: Double, createdDate: LocalDateTime, transactionDate: LocalDateTime) {
  def matchesQuery: Boolean = scoreNumber > 0
}
private object QueryScore {
  def apply(transaction: Transaction, queryParts: Seq[String])(implicit accountingConfig: Config): QueryScore = {
    def scorePart(queryPart: String): Double = {
      def splitToParts(s: String): Seq[String] = {
        Splitter.onPattern("[ ,.]")
          .trimResults()
          .omitEmptyStrings()
          .split(s)
          .asScala
          .toVector
      }
      val searchableParts: Seq[String] = Seq(
        transaction.description,
        transaction.detailDescription,
        transaction.tagsString,
        transaction.beneficiary.longName,
        transaction.moneyReservoir.name,
        transaction.category.name)
        .flatMap(splitToParts)
        .map(_.toLowerCase)

      var score: Double = 0
      if (searchableParts contains queryPart.toLowerCase) {
        score += 2
      } else if (searchableParts.map(_ contains queryPart.toLowerCase) contains true) {
        score += 1
      }

      def stripSign(s: String) = s.replace("-", "")
      val flowAsString = transaction.flow.formatFloat
      if (flowAsString == queryPart) {
        score += 3
      } else if (stripSign(flowAsString) == stripSign(queryPart)) {
        score += 2.5
      } else if (flowAsString contains queryPart) {
        score += 0.1
      }
      score
    }
    val scoreNumber = queryParts.map(scorePart).sum
    QueryScore(scoreNumber, transaction.createdDate, transaction.consumedDate)
  }

  implicit def defaultOrdering: Ordering[QueryScore] = Ordering.by(QueryScore.unapply)
}

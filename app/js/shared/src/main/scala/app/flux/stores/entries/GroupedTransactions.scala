package app.flux.stores.entries

import common.money.ExchangeRateManager
import common.money.Money
import common.money.MoneyWithGeneralCurrency
import common.time.JavaTimeImplicits._
import common.time.LocalDateTime
import app.models.access.EntityAccess
import app.models.accounting.Transaction
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.user.User

import scala.annotation.tailrec
import scala.collection.immutable.Seq

abstract class GroupedTransactions(val transactions: Seq[Transaction]) {
  require(
    transactions.map(_.transactionGroupId).distinct.size == 1,
    s"More than one transaction group: ${transactions.map(_.transactionGroupId).distinct}")

  def groupId: Long = transactions.head.transactionGroupId
  def issuer(implicit entityAccess: EntityAccess): User = transactions.head.issuer
  def transactionDates: Seq[LocalDateTime] = transactions.map(_.transactionDate).distinct
  def consumedDates: Seq[LocalDateTime] = transactions.flatMap(_.consumedDateOption).distinct
  def beneficiaries(implicit accountingConfig: Config): Seq[Account] =
    transactions.map(_.beneficiary).distinct
  def moneyReservoirs(implicit accountingConfig: Config): Seq[MoneyReservoir] =
    transactions.map(_.moneyReservoir).distinct
  def categories(implicit accountingConfig: Config): Seq[Category] =
    transactions.map(_.category).distinct
  def description: String = {
    val descriptions = transactions.map(_.description).distinct
    val prefix = {
      val rawPrefix = longestCommonPrefix(descriptions)
      if (descriptions contains rawPrefix) rawPrefix else removeRightWord(rawPrefix)
    }

    descriptions match {
      case Seq(description) => description

      case _ if prefix.nonEmpty =>
        val suffixes = descriptions.toStream.map(_.substring(prefix.length)).filter(_.nonEmpty).toVector
        s"$prefix{${suffixes.mkString(", ")}}"

      case _ => descriptions.mkString(", ")
    }
  }
  def mostRecentTransaction: Transaction = transactions.maxBy(_.transactionDate)
  def tags: Seq[String] = transactions.flatMap(_.tags).distinct

  def flow(implicit exchangeRateManager: ExchangeRateManager, accountingConfig: Config): Money = {
    val currencies = transactions.map(_.flow.currency).distinct
    currencies match {
      case Seq(currency) => // All transactions have the same currency
        val dates = transactions.map(_.transactionDate).distinct
        val flow: MoneyWithGeneralCurrency =
          transactions.map(_.flow).sum(MoneyWithGeneralCurrency.numeric(currency))
        if (dates.size == 1) {
          // All transactions have the same date, so this should be a DatedMoney
          flow.withDate(dates.head)
        } else {
          // Dates differ, so the best we can do is general Money
          flow
        }
      case _ => // Multiple currencies --> only show reference currency
        transactions.map(_.flow.exchangedForReferenceCurrency).sum
    }
  }

  @tailrec
  private def longestCommonPrefix(strings: Seq[String], nextIndex: Int = 0): String = {
    val nextChars = strings.toStream
      .map {
        case s if nextIndex < s.length => Some(s(nextIndex))
        case _                         => None
      }
      .distinct
      .toVector
    nextChars match {
      case Vector(Some(_)) => longestCommonPrefix(strings, nextIndex = nextIndex + 1)
      case _               => (strings.headOption getOrElse "").substring(0, nextIndex)
    }
  }

  @tailrec
  private def removeRightWord(string: String): String = {
    string.lastOption match {
      case Some(c) if c.isLetterOrDigit => removeRightWord(string.substring(0, string.length - 1))
      case _                            => string
    }
  }
}

object GroupedTransactions {
  def combineConsecutiveOfSameGroup[T <: GroupedTransactions](entries: Seq[T])(
      combine: (T, T) => T): List[T] = {
    // recursion does not lead to growing stack because of Stream
    def combineToStream(nextEntries: List[T]): Stream[T] = nextEntries match {
      case x :: y :: rest if x.groupId == y.groupId =>
        combineToStream(combine(x, y) :: rest)
      case entry :: rest =>
        entry #:: combineToStream(rest)
      case Nil =>
        Stream.empty
    }
    combineToStream(entries.toList).toList
  }
}

package controllers.helpers.accounting

import collection.immutable.Seq
import org.joda.time.Instant
import com.github.nscala_time.time.Imports._
import common.Clock
import models._
import models.accounting.{Tag, Transaction}
import models.accounting.config.{Account, Category, MoneyReservoir, Config}
import models.accounting.money.{DatedMoney, Money, MoneyWithGeneralCurrency, ReferenceMoney, ExchangeRateManager}

abstract class GroupedTransactions(val transactions: Seq[Transaction]) {
  def groupId = transactions(0).transactionGroupId
  def issuer(implicit entityAccess: EntityAccess): User = transactions(0).issuer
  def transactionDates: Seq[Instant] = transactions.map(_.transactionDate).distinct
  def consumedDates: Seq[Instant] = transactions.flatMap(_.consumedDateOption).distinct
  def beneficiaries(implicit accountingConfig: Config): Seq[Account] =
    transactions.map(_.beneficiary).distinct
  def moneyReservoirs(implicit accountingConfig: Config): Seq[MoneyReservoir] =
    transactions.map(_.moneyReservoir).distinct
  def categories(implicit accountingConfig: Config): Seq[Category] =
    transactions.map(_.category).distinct
  def descriptions: Seq[String] = transactions.map(_.description).distinct
  def mostRecentTransaction: Transaction = transactions.sortBy(_.transactionDate).last
  def tags: Seq[Tag] = transactions.flatMap(_.tags).distinct

  def flow(implicit exchangeRateManager: ExchangeRateManager,
           accountingConfig: Config): Money = {
    val currencies = transactions.map(_.flow.currency).distinct
    currencies match {
      case Seq(currency) => // All transactions have the same currency
        val dates = transactions.map(_.transactionDate).distinct
        val flow: MoneyWithGeneralCurrency = transactions.map(_.flow).sum(MoneyWithGeneralCurrency.numeric(currency))
        if (dates.size == 1) {
          // All transactions have the same date, so this should be a DatedMoney
          flow.withDate(dates(0))
        } else {
          // Dates differ, so the best we can do is general Money
          flow
        }
      case _ => // Multiple currencies --> only show reference currency
        transactions.map(_.flow.exchangedForReferenceCurrency).sum
    }
  }
}

object GroupedTransactions {
  def combineConsecutiveOfSameGroup[T <: GroupedTransactions](entries: Seq[T])(combine: (T, T) => T): List[T] = {
    // recursion does not lead to growing stack because of Stream
    def combineToStream(nextEntries: List[T]): Stream[T] = nextEntries match {
      case x :: y :: rest if (x.groupId == y.groupId) =>
        combineToStream(combine(x, y) :: rest)
      case entry :: rest =>
        entry #:: combineToStream(rest)
      case Nil =>
        Stream.empty
    }
    combineToStream(entries.toList).toList
  }
}

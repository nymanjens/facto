package flux.stores.entries

import common.accounting.Tag
import common.time.JavaTimeImplicits._
import common.time.LocalDateTime
import models._
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{ExchangeRateManager, Money, MoneyWithGeneralCurrency}
import models.accounting.Transaction

import scala.collection.immutable.Seq

abstract class GroupedTransactions(val transactions: Seq[Transaction]) {
  require(
    transactions.map(_.transactionGroupId).distinct.size == 1,
    s"More than one transaction group: ${transactions.map(_.transactionGroupId).distinct}")

  def groupId = transactions.head.transactionGroupId
  def issuer(implicit entityAccess: EntityAccess): User = transactions.head.issuer
  def transactionDates: Seq[LocalDateTime] = transactions.map(_.transactionDate).distinct
  def consumedDates: Seq[LocalDateTime] = transactions.flatMap(_.consumedDateOption).distinct
  def beneficiaries(implicit accountingConfig: Config): Seq[Account] =
    transactions.map(_.beneficiary).distinct
  def moneyReservoirs(implicit accountingConfig: Config): Seq[MoneyReservoir] =
    transactions.map(_.moneyReservoir).distinct
  def categories(implicit accountingConfig: Config): Seq[Category] =
    transactions.map(_.category).distinct
  def descriptions: Seq[String] = transactions.map(_.description).distinct
  def mostRecentTransaction: Transaction = transactions.sortBy(_.transactionDate).last
  def tags: Seq[Tag] = transactions.flatMap(_.tags).distinct

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

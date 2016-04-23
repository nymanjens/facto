package controllers.helpers.accounting

import collection.immutable.Seq

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

import models.User
import models.accounting.{Transaction, Money}
import models.accounting.config.{Account, MoneyReservoir, Category}

abstract class GroupedTransactions(val transactions: Seq[Transaction]) {
  def groupId = transactions(0).transactionGroupId

  def issuer: User = transactions(0).issuer

  def transactionDates: Seq[DateTime] = transactions.map(_.transactionDate).distinct

  def consumedDates: Seq[DateTime] = transactions.flatMap(_.consumedDateOption).distinct

  def beneficiaries: Seq[Account] = transactions.map(_.beneficiary).distinct

  def moneyReservoirs: Seq[MoneyReservoir] = transactions.map(_.moneyReservoir).distinct

  def categories: Seq[Category] = transactions.map(_.category).distinct

  def descriptions: Seq[String] = transactions.map(_.description).distinct

  def flow: Money = transactions.map(_.flow).sum

  def mostRecentTransaction: Transaction = transactions.sortBy(_.transactionDate).last
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
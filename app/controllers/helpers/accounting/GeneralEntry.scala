package controllers.helpers.accounting

import collection.immutable.Seq

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.accounting.{Transaction, Transactions}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}

case class GeneralEntry(override val transactions: Seq[Transaction])
  extends GroupedTransactions(transactions)

object GeneralEntry {

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(n: Int): Seq[GeneralEntry] = {
    val transactions: Seq[Transaction] =
      Transactions.fetchAll(_.
        sortBy(_.transactionDate)(Ordering[DateTime].reverse)
        .take(3 * n)
        .reverse)

    var entries = transactions.map(t => GeneralEntry(Seq(t)))

    entries = combineConsecutiveOfSameGroup(entries)

    entries.takeRight(n)
  }

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEndowments(account: Account, n: Int) = {
    val transactions: Seq[Transaction] =
      dbRun(
        Transactions.newQuery
          .filter(_.categoryCode === Config.constants.endowmentCategory.code)
          .filter(_.beneficiaryAccountCode === account.code)
          .sortBy(r => (r.consumedDate.desc, r.createdDate.desc))
          .take(3 * n))
        .reverse
        .toList

    var entries = transactions.map(t => GeneralEntry(Seq(t)))

    entries = combineConsecutiveOfSameGroup(entries)

    entries.takeRight(n)
  }

  private[accounting] def combineConsecutiveOfSameGroup(entries: Seq[GeneralEntry]): Seq[GeneralEntry] = {
    GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
      /* combine */ (first, last) => GeneralEntry(first.transactions ++ last.transactions)
    }
  }
}

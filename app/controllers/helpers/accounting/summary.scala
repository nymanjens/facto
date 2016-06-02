package controllers.helpers.accounting

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

import com.google.common.collect.{HashMultimap, ImmutableTable, Tables, Table, Multimap, Range}
import org.joda.time.DateTime
import models.SlickUtils.dbApi._
import com.github.nscala_time.time.Imports._

import play.api.Logger
import play.twirl.api.Html

import common.{Clock, MonthRange, DatedMonth}
import common.CollectionUtils.toListMap
import common.GuavaUtils.asGuava
import models.SlickUtils.{dbRun, JodaToSqlDateMapper}
import models.accounting.{Transactions, Transaction, Money}
import models.accounting.config.{Category, Account}
import models.accounting.config.Account.SummaryTotalRowDef

case class Summary(yearToSummary: Map[Int, SummaryForYear],
                   categories: Seq[Category],
                   monthRangeForAverages: MonthRange) {
  def totalRowTitles: Seq[Html] = {
    val firstSummary = yearToSummary.values.iterator.next
    firstSummary.totalRows.map(_.rowTitleHtml)
  }
}

object Summary {
  def fetchSummary(account: Account, expandedYear: Int): Summary = {
    val now = Clock.now
    val allTransactions = dbRun(
      Transactions.newQuery
        .filter(_.beneficiaryAccountCode === account.code)
        .sortBy(r => (r.consumedDate, r.createdDate)))
      .toList
    val yearToTransactions: Map[Int, Seq[Transaction]] = allTransactions.groupBy(t => t.consumedDate.getYear)
    val years: Seq[Int] = {
      val yearsSet = yearToTransactions.keySet ++ Set(now.getYear, expandedYear)
      yearsSet.toList.sorted
    }
    val monthRangeForAverages: MonthRange = {
      allTransactions.headOption.map(_.consumedDate) match {
        case Some(firstDate) =>
          val atLeastFirst = MonthRange.atLeast(DatedMonth.containing(firstDate))
          val atMostLastMonth = MonthRange.lessThan(DatedMonth.containing(now))
          atLeastFirst intersection atMostLastMonth
        case _ =>
          MonthRange.empty
      }
    }

    val yearToSummary: Map[Int, SummaryForYear] = toListMap {
      for ((year, i) <- years.zipWithIndex) yield {
        val transactions = yearToTransactions.get(year) getOrElse Seq()
        year -> SummaryForYear.fromTransactions(transactions, account, monthRangeForAverages, year)
      }
    }

    val categories: Seq[Category] = account.categories.filter { category =>
      !yearToSummary.values.filter(_.hasEntries(category)).isEmpty
    }

    Summary(yearToSummary, categories, monthRangeForAverages)
  }
}

case class SummaryForYear(cells: ImmutableTable[Category, DatedMonth, SummaryCell],
                          categoryToAverages: Map[Category, Money],
                          totalRows: Seq[SummaryTotalRow]) {

  def months: Seq[DatedMonth] = cells.columnKeySet().asScala.toList

  def categories: Iterable[Category] = categoryToAverages.keys

  def cell(category: Category, month: DatedMonth): SummaryCell = cells.get(category, month)

  def hasEntries(category: Category): Boolean = {
    val entries = for {
      cell <- cells.row(category).values.asScala
      entry <- cell.entries
    } yield entry
    !entries.isEmpty
  }
}

object SummaryForYear {

  private[accounting] def fromTransactions(transactions: Seq[Transaction], account: Account, monthRangeForAverages: MonthRange, year: Int): SummaryForYear = {
    val summaryBuilder = new SummaryForYear.Builder(account, monthRangeForAverages, year)
    for (transaction <- transactions) {
      summaryBuilder.addTransaction(transaction)
    }
    summaryBuilder.result
  }

  private class Builder(account: Account, monthRangeForAverages: MonthRange, year: Int) {
    private val cellBuilders: ImmutableTable[Category, DatedMonth, SummaryCell.Builder] = {
      val tableBuilder = ImmutableTable.builder[Category, DatedMonth, SummaryCell.Builder]()
      for (category <- account.categories) {
        for (month <- DatedMonth.allMonthsIn(year)) {
          tableBuilder.put(category, month, new SummaryCell.Builder)
        }
      }
      tableBuilder.build()
    }

    private val categoryToTransactions: Multimap[Category, Transaction] = HashMultimap.create()

    def addTransaction(transaction: Transaction): Builder = {
      if (account.categories contains transaction.category) {
        cellBuilders.get(transaction.category, DatedMonth.containing(transaction.consumedDate)).addTransaction(transaction)
        if (monthRangeForAverages contains transaction.consumedDate) {
          categoryToTransactions.put(transaction.category, transaction)
        }
      } else {
        Logger.warn(s"There was a transaction with category ${transaction.category.name}, but this account " +
          s"(${account.longName}) doesn't or no longer contains this category")
      }
      this
    }

    def result: SummaryForYear = {
      val cells = Tables.transformValues(cellBuilders, asGuava[SummaryCell.Builder, SummaryCell](_.result))
      val categoryToAverages: Map[Category, Money] = {
        toListMap {
          for (category <- account.categories) yield {
            val transactions = categoryToTransactions.get(category).asScala
            val totalFlow = transactions.map(_.flow).sum
            val numMonths = (monthRangeForAverages intersection MonthRange.forYear(year)).countMonths
            val average = if (numMonths > 0) totalFlow / numMonths else Money(0)
            category -> average
          }
        }
      }
      val summaryTotals: Seq[SummaryTotalRow] =
        for (summaryTotalRowDef <- account.summaryTotalRows)
          yield SummaryTotalRow.calculate(summaryTotalRowDef, cells, categoryToAverages)
      SummaryForYear(ImmutableTable.copyOf(cells), categoryToAverages, summaryTotals)
    }
  }

}

case class SummaryCell(entries: Seq[GeneralEntry]) {
  def totalFlow: Money = {
    {
      for {
        entry <- entries
        transaction <- entry.transactions
      } yield transaction.flow
    }.sum
  }
}

object SummaryCell {

  private[accounting] class Builder {
    private val transactions = Seq.newBuilder[Transaction]

    def addTransaction(transaction: Transaction): Builder = {
      transactions += transaction
      this
    }

    def result: SummaryCell = {
      var entries = transactions.result().map(t => GeneralEntry(Seq(t)))
      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)
      SummaryCell(entries)
    }
  }
}

case class SummaryTotalRow(rowTitleHtml: Html, monthToTotal: Map[DatedMonth, Money], yearlyAverage: Money)

object SummaryTotalRow {
  def calculate(totalRowDef: SummaryTotalRowDef,
                cells: Table[Category, DatedMonth, SummaryCell],
                categoryToAverages: Map[Category, Money]): SummaryTotalRow = {
    def sumNonIgnoredCategories(categoryToMoney: Map[Category, Money]): Money = {
      categoryToMoney.filter { case (cat, _) => !totalRowDef.categoriesToIgnore.contains(cat) }.values.sum
    }
    val monthToTotal: Map[DatedMonth, Money] = toListMap {
      for ((month, categoryToCell) <- cells.columnMap().asScala.toSeq.sortBy(_._1))
        yield month -> sumNonIgnoredCategories(
          categoryToMoney = categoryToCell.asScala.toMap.mapValues(_.totalFlow))
    }
    val yearlyAverage: Money = sumNonIgnoredCategories(categoryToAverages)
    SummaryTotalRow(
      rowTitleHtml = totalRowDef.rowTitleHtml,
      monthToTotal = monthToTotal,
      yearlyAverage = yearlyAverage)
  }
}

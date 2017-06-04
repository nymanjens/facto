package controllers.helpers.accounting

import com.google.inject.{Inject, Singleton}
import java.time.{LocalDate, Month}
import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import com.google.common.collect.{HashMultimap, ImmutableTable, Multimap, Range, Table, Tables}
import models.SlickUtils.dbApi._

import play.api.Logger
import common.time.LocalDateTime
import common.time.JavaTimeImplicits._
import common.time.{Clock, DatedMonth, MonthRange, TimeUtils, LocalDateTimes}
import common.CollectionUtils.toListMap
import common.GuavaUtils.asGuava
import models.SlickUtils.{localDateTimeToSqlDateMapper, dbRun}
import models.accounting.{Tag, Transaction, SlickTransactionManager}
import models.accounting.config.{Account, Category, Config}
import models.accounting.config.Account.SummaryTotalRowDef
import controllers.helpers.ControllerHelperCache
import controllers.helpers.ControllerHelperCache.CacheIdentifier
import models.accounting.money.{ReferenceMoney, ExchangeRateManager}
import models.SlickEntityAccess

case class Summary(yearToSummary: Map[Int, SummaryForYear],
                   categories: Seq[Category],
                   monthRangeForAverages: MonthRange) {
  def totalRowTitles: Seq[String] = {
    val firstSummary = yearToSummary.values.iterator.next
    firstSummary.totalRows.map(_.rowTitleHtml)
  }
}

final class Summaries @Inject()(implicit accountingConfig: Config,
                                clock: Clock,
                                entityAccess: SlickEntityAccess,
                                exchangeRateManager: ExchangeRateManager,
                                transactionManager: SlickTransactionManager) {
  def fetchSummary(account: Account, expandedYear: Int, tags: Seq[Tag] = Seq()): Summary = {
    val now = clock.now.toLocalDate

    val years: Seq[Int] = getSummaryYears(account, expandedYear, now.getYear)
    val monthRangeForAverages: MonthRange = {
      val oldestTransaction = dbRun(
        transactionManager.newQuery
          .filter(_.beneficiaryAccountCode === account.code)
          .sortBy(_.consumedDate)
          .take(1)).headOption
      oldestTransaction.map(_.consumedDate) match {
        case Some(firstDateTime) =>
          val firstDate = firstDateTime.toLocalDate
          val atLeastFirst = MonthRange.atLeast(DatedMonth.containing(firstDate))
          val atMostLastMonth = MonthRange.lessThan(DatedMonth.containing(now))
          atLeastFirst intersection atMostLastMonth
        case _ =>
          MonthRange.empty
      }
    }

    val yearToSummary: Map[Int, SummaryForYear] = toListMap {
      for ((year, i) <- years.zipWithIndex) yield {
        year -> SummaryForYear.fetch(account, monthRangeForAverages, year, tags)
      }
    }

    val categories: Seq[Category] = account.categories.filter { category =>
      !yearToSummary.values.filter(_.hasEntries(category)).isEmpty
    }

    Summary(yearToSummary, categories, monthRangeForAverages)
  }

  private def getSummaryYears(account: Account, expandedYear: Int, thisYear: Int): Seq[Int] =
    ControllerHelperCache.cached(GetSummaryYears(account, expandedYear, thisYear)) {
      val allTransactions = dbRun(
        transactionManager.newQuery
          .filter(_.beneficiaryAccountCode === account.code))
      val transactionYears = allTransactions.toStream.map(t => t.consumedDate.getYear).toSet
      val yearsSet = transactionYears ++ Set(thisYear, expandedYear)
      yearsSet.toList.sorted

    }
}

private case class GetSummaryYears(account: Account, expandedYear: Int, thisYear: Int)
    extends CacheIdentifier[Seq[Int]] {
  protected override def invalidateWhenUpdatingEntity(oldYears: Seq[Int]) = {
    case transaction: Transaction =>
      transaction.beneficiaryAccountCode == account.code && !oldYears.contains(
        transaction.consumedDate.getYear)
  }
}

case class SummaryForYear(cells: ImmutableTable[Category, DatedMonth, SummaryCell],
                          categoryToAverages: Map[Category, ReferenceMoney],
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

  private[accounting] def fetch(account: Account,
                                monthRangeForAverages: MonthRange,
                                year: Int,
                                tags: Seq[Tag])(implicit accountingConfig: Config,
                                                exchangeRateManager: ExchangeRateManager,
                                                entityAccess: SlickEntityAccess): SummaryForYear =
    ControllerHelperCache.cached(GetSummaryForYear(account, monthRangeForAverages, year, tags)) {
      val transactions: Seq[Transaction] = {
        val yearRange = MonthRange.forYear(year)
        val allTransactions = dbRun(
          entityAccess.transactionManager.newQuery
            .filter(_.beneficiaryAccountCode === account.code)
            .filter(_.consumedDate >= LocalDateTimes.ofJavaLocalDateTime(yearRange.start.atStartOfDay()))
            .filter(
              _.consumedDate < LocalDateTimes.ofJavaLocalDateTime(yearRange.startOfNextMonth.atStartOfDay()))
            .sortBy(r => (r.consumedDate, r.createdDate))).toList
        if (tags.isEmpty) {
          allTransactions // don't filter
        } else {
          def containsAnyTag(seq: Seq[Tag]): Boolean = !seq.filter(tags.contains).isEmpty
          allTransactions.filter(trans => containsAnyTag(trans.tags))
        }
      }

      val summaryBuilder = new SummaryForYear.Builder(account, monthRangeForAverages, year)
      for (transaction <- transactions) {
        summaryBuilder.addTransaction(transaction)
      }
      summaryBuilder.result
    }

  private case class GetSummaryForYear(account: Account,
                                       monthRangeForAverages: MonthRange,
                                       year: Int,
                                       tags: Seq[Tag])
      extends CacheIdentifier[SummaryForYear] {
    protected override def invalidateWhenUpdating = {
      case transaction: Transaction =>
        transaction.beneficiaryAccountCode == account.code && transaction.consumedDate.getYear == year
    }
  }

  private class Builder(account: Account, monthRangeForAverages: MonthRange, year: Int)(
      implicit accountingConfig: Config,
      exchangeRateManager: ExchangeRateManager) {
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
        cellBuilders
          .get(transaction.category, DatedMonth.containing(transaction.consumedDate))
          .addTransaction(transaction)
        if (monthRangeForAverages contains transaction.consumedDate) {
          categoryToTransactions.put(transaction.category, transaction)
        }
      } else {
        Logger.warn(
          s"There was a transaction with category ${transaction.category.name}, but this account " +
            s"(${account.longName}) doesn't or no longer contains this category")
      }
      this
    }

    def result: SummaryForYear = {
      val cells = Tables.transformValues(cellBuilders, asGuava[SummaryCell.Builder, SummaryCell](_.result))
      val categoryToAverages: Map[Category, ReferenceMoney] = {
        toListMap {
          for (category <- account.categories) yield {
            val transactions = categoryToTransactions.get(category).asScala
            val totalFlow = transactions.map(_.flow.exchangedForReferenceCurrency).sum
            val numMonths = (monthRangeForAverages intersection MonthRange.forYear(year)).countMonths
            val average = if (numMonths > 0) totalFlow / numMonths else ReferenceMoney(0)
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
  def totalFlow(implicit exchangeRateManager: ExchangeRateManager,
                accountingConfig: Config): ReferenceMoney = {
    {
      for {
        entry <- entries
        transaction <- entry.transactions
      } yield transaction.flow.exchangedForReferenceCurrency
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
      // Disabled combining because this often leads to confusing situations rather than helping
      //entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)
      SummaryCell(entries)
    }
  }
}

case class SummaryTotalRow(rowTitleHtml: String,
                           monthToTotal: Map[DatedMonth, ReferenceMoney],
                           yearlyAverage: ReferenceMoney)

object SummaryTotalRow {
  def calculate(totalRowDef: SummaryTotalRowDef,
                cells: Table[Category, DatedMonth, SummaryCell],
                categoryToAverages: Map[Category, ReferenceMoney])(
      implicit exchangeRateManager: ExchangeRateManager,
      accountingConfig: Config): SummaryTotalRow = {
    def sumNonIgnoredCategories(categoryToMoney: Map[Category, ReferenceMoney]): ReferenceMoney = {
      categoryToMoney.filter { case (cat, _) => !totalRowDef.categoriesToIgnore.contains(cat) }.values.sum
    }
    val monthToTotal: Map[DatedMonth, ReferenceMoney] = toListMap {
      for ((month, categoryToCell) <- cells.columnMap().asScala.toSeq.sortBy(_._1))
        yield
          month -> sumNonIgnoredCategories(
            categoryToMoney = categoryToCell.asScala.toMap.mapValues(_.totalFlow))
    }
    val yearlyAverage: ReferenceMoney = sumNonIgnoredCategories(categoryToAverages)
    SummaryTotalRow(
      rowTitleHtml = totalRowDef.rowTitleHtml,
      monthToTotal = monthToTotal,
      yearlyAverage = yearlyAverage)
  }
}

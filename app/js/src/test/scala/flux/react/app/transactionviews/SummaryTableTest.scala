package flux.react.app.transactionviews

import java.time.Month._

import common.GuavaReplacement.DoubleMath.roundToLong
import common.money.{Currency, ReferenceMoney}
import common.testing.TestModule
import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import common.time.{DatedMonth, YearRange}
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.{GainsForMonth, GainsForYear}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import models.accounting._
import utest._

import scala.collection.immutable.{ListMap, Seq}
import scala2js.Converters._

object SummaryTableTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val summaryTable = testModule.summaryTable
    implicit val fakeClock = testModule.fakeClock
    fakeClock.setTime(createDateTime(2013, JANUARY, 2))

    val allYearsData = summaryTable.AllYearsData(
      allTransactionsYearRange = YearRange.closed(2010, 2013),
      yearsToData = ListMap(
        2012 -> summaryTable.AllYearsData.YearData(
          SummaryForYear(
            Seq(
              createTransaction(year = 2012, month = APRIL, flow = 22, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = 1.2, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = -2, category = testCategoryC)
            )),
          GainsForYear(
            monthToGains = Map(DatedMonth.of(2012, JUNE) ->
              GainsForMonth.forSingle(testReservoirCashGbp, ReferenceMoney(123))),
            impactingTransactionIds = Set(),
            impactingBalanceCheckIds = Set()
          )
        ),
        2013 -> summaryTable.AllYearsData.YearData(
          SummaryForYear(
            Seq(
              createTransaction(year = 2013, category = testCategoryA),
              createTransaction(year = 2013, category = testCategoryB))),
          GainsForYear.empty
        )
      ),
      netWorth = ReferenceMoney(23737373)
    )

    "AllYearsData" - {
      "categories" - {
        implicit val account = testAccount.copy(categories = Seq(testCategoryB, testCategoryA))

        allYearsData.categories ==> Seq(testCategoryB, testCategoryA, testCategoryC)
      }
      "cell" - {
        val cell = allYearsData.cell(testCategoryA, DatedMonth.of(2012, JUNE))
        cell.transactions.size ==> 1
        cell.transactions(0).flow.cents ==> 120

        // Note: The SummaryCell methods are tested in SummaryForYearStoreFactoryTest
      }
      "totalWithoutCategories" - {
        allYearsData
          .totalWithoutCategories(categoriesToIgnore = Set(), month = DatedMonth.of(2012, JUNE)) ==>
          ReferenceMoney(120 - 200 + 123)
        allYearsData
          .totalWithoutCategories(categoriesToIgnore = Set(testCategoryC), month = DatedMonth.of(2012, JUNE)) ==>
          ReferenceMoney(120 + 123)
      }
      "averageWithoutCategories" - {
        "full year" - {
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 12))
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 12))
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))

          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))

          newAllYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 9))
          newAllYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 9))
        }
      }
      "years" - {
        allYearsData.years ==> Seq(2012, 2013)
      }
      "yearlyAverage" - {
        "full year" - {
          allYearsData.yearlyAverage(2012, testCategoryA) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 12))
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))
          allYearsData.yearlyAverage(2012, testCategoryA) ==> ReferenceMoney(roundToLong(2200.0 / 5))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))
          newAllYearsData.yearlyAverage(2012, testCategoryA) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 9))
        }
      }
      "monthsForAverage" - {
        "full year" - {
          allYearsData.monthsForAverage(2012) ==> DatedMonth.allMonthsIn(2012)
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))
          allYearsData.monthsForAverage(2012) ==>
            Seq(
              DatedMonth.of(2012, JANUARY),
              DatedMonth.of(2012, FEBRUARY),
              DatedMonth.of(2012, MARCH),
              DatedMonth.of(2012, APRIL),
              DatedMonth.of(2012, MAY))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))

          newAllYearsData.monthsForAverage(2012) ==>
            Seq(
              DatedMonth.of(2012, APRIL),
              DatedMonth.of(2012, MAY),
              DatedMonth.of(2012, JUNE),
              DatedMonth.of(2012, JULY),
              DatedMonth.of(2012, AUGUST),
              DatedMonth.of(2012, SEPTEMBER),
              DatedMonth.of(2012, OCTOBER),
              DatedMonth.of(2012, NOVEMBER),
              DatedMonth.of(2012, DECEMBER)
            )
        }
      }
      "currenciesWithExchangeRateGains" - {
        allYearsData.currenciesWithExchangeRateGains ==> Seq(Currency.Gbp)
      }
      "exchangeRateGains" - {
        allYearsData.exchangeRateGains(Currency.Gbp, DatedMonth.of(2012, JUNE)) ==> ReferenceMoney(123)
        allYearsData.exchangeRateGains(Currency.Gbp, DatedMonth.of(2012, AUGUST)) ==> ReferenceMoney(0)
      }
      "averageExchangeRateGains" - {
        "full year" - {
          allYearsData.averageExchangeRateGains(Currency.Gbp, 2012) ==> ReferenceMoney(roundToLong(123 / 12))
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))
          allYearsData.averageExchangeRateGains(Currency.Gbp, 2012) ==> ReferenceMoney(0)
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))
          newAllYearsData.averageExchangeRateGains(Currency.Gbp, 2012) ==>
            ReferenceMoney(roundToLong(123.0 / 9))
        }
      }
    }
  }

  private final class ThisTestModule extends TestModule {

    import com.softwaremill.macwire._

    private val storesModule = new flux.stores.Module

    implicit val summaryYearsStoreFactory = storesModule.summaryYearsStoreFactory
    implicit val summaryForYearStoreFactory = storesModule.summaryForYearStoreFactory
    implicit val summaryExchangeRateGainsStoreFactory = storesModule.summaryExchangeRateGainsStoreFactory
    implicit val cashFlowEntriesStoreFactory = storesModule.cashFlowEntriesStoreFactory

    val summaryTable: SummaryTable = wire[SummaryTable]
  }
}

package app.flux.react.app.transactionviews

import java.time.Month._
import hydro.common.GuavaReplacement.DoubleMath.roundToLong
import app.common.money.Currency
import app.common.money.ReferenceMoney
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.common.time.AccountingYear
import app.common.time.DatedMonth
import app.common.time.YearRange
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.GainsForMonth
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.ExchangeRateGains
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import app.flux.stores.entries.factories.SummaryInflationGainsStoreFactory.InflationGains
import hydro.common.time.LocalDateTimes.createDateTime
import utest._

import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq

object SummaryTableTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val summaryTable = testModule.summaryTable
    implicit val fakeClock = testModule.fakeClock
    fakeClock.setNow(createDateTime(2013, JANUARY, 2))

    val allYearsData = summaryTable.AllYearsData(
      allTransactionsYearRange = YearRange.closed(AccountingYear(2010), AccountingYear(2013)),
      yearsToData = ListMap(
        AccountingYear(2012) -> summaryTable.AllYearsData.YearData(
          SummaryForYear(
            Seq(
              createTransaction(year = 2012, month = APRIL, flow = 22, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = 1.2, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = -2, category = testCategoryC),
            )
          ),
          exchangeRateGains = ExchangeRateGains(
            monthToGains = Map(
              DatedMonth.of(2012, JUNE) ->
                GainsForMonth.forSingle(testReservoirCashGbp, ReferenceMoney(123))
            ),
            impactingTransactionIds = Set(),
            impactingBalanceCheckIds = Set(),
          ),
          exchangeRateGainsCorrectedForInflation = ExchangeRateGains.empty,
          inflationGains = InflationGains.empty,
        ),
        AccountingYear( 2013) -> summaryTable.AllYearsData.YearData(
          summary = SummaryForYear(
            Seq(
              createTransaction(year = 2013, category = testCategoryA),
              createTransaction(year = 2013, category = testCategoryB),
            )
          ),
          exchangeRateGains = ExchangeRateGains.empty,
          exchangeRateGainsCorrectedForInflation = ExchangeRateGains.empty,
          inflationGains = InflationGains.empty,
        ),
      ),
      netWorth = ReferenceMoney(23737373),
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
          .totalWithoutCategories(
            categoriesToIgnore = Set(),
            month = DatedMonth.of(2012, JUNE),
            correctForInflation = false,
          ) ==>
          ReferenceMoney(120 - 200 + 123)
        allYearsData
          .totalWithoutCategories(
            categoriesToIgnore = Set(testCategoryC),
            month = DatedMonth.of(2012, JUNE),
            correctForInflation = false,
          ) ==>
          ReferenceMoney(120 + 123)
      }
      "averageWithoutCategories" - {
        "full year" - {
          allYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 12))
          allYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(testCategoryC),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 12))
        }
        "only before June" - {
          fakeClock.setNow(createDateTime(2012, JUNE, 2))

          allYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
          allYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(testCategoryC),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange =
            YearRange.closed(AccountingYear(2012), AccountingYear(2013))
          )

          newAllYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 9))
          newAllYearsData
            .averageWithoutCategories(
              categoriesToIgnore = Set(testCategoryC),
              accountingYear = AccountingYear(2012),
              correctForInflation = false,
            ) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 9))
        }
      }
      "years" - {
        allYearsData.years ==> Seq(AccountingYear( 2012), AccountingYear(2013))
      }
      "yearlyAverage" - {
        "full year" - {
          allYearsData.yearlyAverage(AccountingYear(2012), testCategoryA, correctForInflation = false) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 12))
        }
        "only before June" - {
          fakeClock.setNow(createDateTime(2012, JUNE, 2))
          allYearsData.yearlyAverage(
            AccountingYear(2012),
            testCategoryA,
            correctForInflation = false,
          ) ==> ReferenceMoney(
            roundToLong(2200.0 / 5)
          )
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange =
            YearRange.closed(AccountingYear(2012), AccountingYear(2013))
          )
          newAllYearsData.yearlyAverage(AccountingYear(2012), testCategoryA, correctForInflation = false) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 9))
        }
      }
      "monthsForAverage" - {
        "full year" - {
          allYearsData.monthsForAverage(AccountingYear(2012)) ==> DatedMonth.allMonthsIn(AccountingYear(2012))
        }
        "only before June" - {
          fakeClock.setNow(createDateTime(2012, JUNE, 2))
          allYearsData.monthsForAverage(AccountingYear(2012)) ==>
            Seq(
              DatedMonth.of(2012, JANUARY),
              DatedMonth.of(2012, FEBRUARY),
              DatedMonth.of(2012, MARCH),
              DatedMonth.of(2012, APRIL),
              DatedMonth.of(2012, MAY),
            )
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange =
            YearRange.closed(AccountingYear(2012), AccountingYear(2013))
          )

          newAllYearsData.monthsForAverage(AccountingYear(2012)) ==>
            Seq(
              DatedMonth.of(2012, APRIL),
              DatedMonth.of(2012, MAY),
              DatedMonth.of(2012, JUNE),
              DatedMonth.of(2012, JULY),
              DatedMonth.of(2012, AUGUST),
              DatedMonth.of(2012, SEPTEMBER),
              DatedMonth.of(2012, OCTOBER),
              DatedMonth.of(2012, NOVEMBER),
              DatedMonth.of(2012, DECEMBER),
            )
        }
      }
      "currenciesWithExchangeRateGains" - {
        allYearsData.currenciesWithExchangeRateGains ==> Seq(Currency.Gbp)
      }
      "exchangeRateGains" - {
        allYearsData.exchangeRateGains(
          Currency.Gbp,
          DatedMonth.of(2012, JUNE),
          correctForInflation = false,
        ) ==> ReferenceMoney(123)
        allYearsData.exchangeRateGains(
          Currency.Gbp,
          DatedMonth.of(2012, AUGUST),
          correctForInflation = false,
        ) ==> ReferenceMoney(0)
      }
      "averageExchangeRateGains" - {
        "full year" - {
          allYearsData.averageExchangeRateGains(
            Currency.Gbp,
            AccountingYear(2012),
            correctForInflation = false,
          ) ==> ReferenceMoney(roundToLong(123 / 12))
        }
        "only before June" - {
          fakeClock.setNow(createDateTime(2012, JUNE, 2))
          allYearsData.averageExchangeRateGains(
            Currency.Gbp,
            AccountingYear(2012),
            correctForInflation = false,
          ) ==> ReferenceMoney(0)
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange =
            YearRange.closed(AccountingYear(2012), AccountingYear(2013))
          )
          newAllYearsData.averageExchangeRateGains(
            Currency.Gbp,
            AccountingYear(2012),
            correctForInflation = false,
          ) ==>
            ReferenceMoney(roundToLong(123.0 / 9))
        }
      }
    }
  }

  private final class ThisTestModule extends TestModule {

    private val storesModule = new app.flux.stores.Module

    implicit val summaryYearsStoreFactory = storesModule.summaryYearsStoreFactory
    implicit val summaryForYearStoreFactory = storesModule.summaryForYearStoreFactory
    implicit val summaryExchangeRateGainsStoreFactory = storesModule.summaryExchangeRateGainsStoreFactory
    implicit val summaryInflationGainsStoreFactory = storesModule.summaryInflationGainsStoreFactory
    implicit val cashFlowEntriesStoreFactory = storesModule.cashFlowEntriesStoreFactory
    implicit val inMemoryUserConfigStore = storesModule.inMemoryUserConfigStore

    private val appCommonAccountingModule = new app.common.accounting.Module()
    implicit val templateMatcher = appCommonAccountingModule.templateMatcher

    val summaryTable: SummaryTable = new SummaryTable
  }
}

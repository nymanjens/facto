package app.flux.stores.entries.factories

import java.time.LocalDate
import java.time.Month._
import app.common.accounting.ComplexQueryFilter
import app.common.money.ReferenceMoney
import hydro.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.common.time.DatedMonth
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.GainsForMonth
import app.flux.stores.entries.AccountingEntryUtils
import app.models.accounting.config.MoneyReservoir
import hydro.models.modification.EntityModification
import hydro.common.time.LocalDateTime
import hydro.common.time.LocalDateTimes.createDateTime
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object SummaryExchangeRateGainsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val fakeClock = testModule.fakeClock
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val exchangeRateManager = testModule.exchangeRateManager
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()
    implicit val accountingEntryUtils = new AccountingEntryUtils()
    val factory: SummaryExchangeRateGainsStoreFactory = new SummaryExchangeRateGainsStoreFactory()

    "no transactions" - async {
      val store = factory.get(testAccountA, year = 2013, correctForInflation = false)

      val exchangeRateGains = await(store.stateFuture)
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2012, DECEMBER, 1))) ==> GainsForMonth.empty
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==> GainsForMonth.empty
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, DECEMBER, 1))) ==> GainsForMonth.empty
    }

    "domestic reservoir" - async {
      persistTransaction(flow = 789, date = createDateTime(2013, JANUARY, 5), reservoir = testReservoirCashA)

      val store = factory.get(testAccountA, year = 2013, correctForInflation = false)
      val exchangeRateGains = await(store.stateFuture)

      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==> GainsForMonth.empty
    }

    "foreign reservoir" - async {
      persistGbpRate(date = createDateTime(2012, JANUARY, 30), ratio = 1.0)
      persistGbpRate(date = createDateTime(2012, DECEMBER, 20), ratio = 1.8)
      persistGbpRate(date = createDateTime(2012, DECEMBER, 30), ratio = 1.1)

      persistGbpRate(date = createDateTime(2013, JANUARY, 2), ratio = 1.0)
      persistGbpRate(date = createDateTime(2013, JANUARY, 30), ratio = 1.2)
      persistGbpRate(date = createDateTime(2013, FEBRUARY, 2), ratio = 1.9)
      persistGbpRate(date = createDateTime(2013, FEBRUARY, 4), ratio = 1.0)
      persistGbpRate(date = createDateTime(2013, FEBRUARY, 27), ratio = 1.3)
      persistGbpRate(date = createDateTime(2013, MARCH, 2), ratio = 1.2)
      persistGbpRate(date = createDateTime(2013, MARCH, 6), ratio = 1.9)
      persistGbpRate(date = createDateTime(2013, MARCH, 30), ratio = 1.4)
      persistGbpRate(date = createDateTime(2013, APRIL, 4), ratio = 1.9)
      persistGbpRate(date = createDateTime(2013, APRIL, 30), ratio = 1.5)
      persistGbpRate(date = createDateTime(2013, MAY, 2), ratio = 1.3)
      persistGbpRate(date = createDateTime(2013, MAY, 6), ratio = 1.7)
      persistGbpRate(date = createDateTime(2013, MAY, 30), ratio = 1.5)
      persistGbpRate(date = createDateTime(2013, JUNE, 30), ratio = 1.6)
      persistGbpRate(date = createDateTime(2013, JULY, 30), ratio = 1.6)
      persistGbpRate(date = createDateTime(2013, AUGUST, 30), ratio = 1.5)

      persistTransaction(flow = 789, date = createDateTime(2012, JANUARY, 5)) // Balance = 789 GBP
      persistBalanceCheck(balance = 100, date = createDateTime(2012, NOVEMBER, 5)) // Balance = 100
      persistTransaction(flow = 20, date = createDateTime(2012, DECEMBER, 5)) // Balance = 120 GBP
      persistTransaction(flow = 20, date = createDateTime(2012, DECEMBER, 5)) // Balance = 140 GBP
      persistTransaction(flow = 80, date = createDateTime(2012, DECEMBER, 5)) // Balance = 200 GBP
      persistBalanceCheck(balance = 200, date = createDateTime(2012, DECEMBER, 25)) // Balance check

      persistTransaction(flow = -400, date = createDateTime(2013, JANUARY, 5)) // Balance = -200 GBP
      persistBalanceCheck(balance = -200, date = createDateTime(2013, FEBRUARY, 2)) // Balance check
      persistBalanceCheck(balance = -100, date = createDateTime(2013, FEBRUARY, 5)) // Balance = -100
      persistTransaction(flow = 200, date = createDateTime(2013, MARCH, 5)) // Balance = 100
      persistBalanceCheck(balance = 100, date = createDateTime(2013, MARCH, 12)) // Balance check
      persistBalanceCheck(balance = 100, date = createDateTime(2013, APRIL, 13)) // Balance check
      persistTransaction(flow = 300, date = createDateTime(2013, MAY, 5)) // Balance = 400
      persistTransaction(flow = -600, date = createDateTime(2013, MAY, 9)) // Balance = -200

      val store = factory.get(testAccountA, year = 2013, correctForInflation = false)
      val exchangeRateGains = await(store.stateFuture)

      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2012, JANUARY, 1))) ==> GainsForMonth.empty
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2012, NOVEMBER, 1))) ==> GainsForMonth.empty
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2012, DECEMBER, 1))) ==> GainsForMonth.empty

      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(20 - 80))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, FEBRUARY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-20 + 30))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, MARCH, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-10 + 40))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, APRIL, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(10))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, MAY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(60 + 120))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, JUNE, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-20))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, JULY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(0))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, AUGUST, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(20))
      exchangeRateGains.gainsForMonth(DatedMonth(LocalDate.of(2013, SEPTEMBER, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(0))
    }
  }

  private def refMoney(double: Double): ReferenceMoney = ReferenceMoney((double * 100).toLong)

  private def persistTransaction(
      flow: Double,
      date: LocalDateTime,
      reservoir: MoneyReservoir = testReservoirCashGbp,
  )(implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addRemotelyAddedEntities(
      createTransaction(flow = flow, reservoir = reservoir).copy(transactionDate = date)
    )
  }
  private def persistBalanceCheck(balance: Double, date: LocalDateTime)(implicit
      entityAccess: FakeJsEntityAccess
  ): Unit = {
    entityAccess.addRemotelyAddedEntities(
      testBalanceCheckWithId.copy(
        idOption = Some(EntityModification.generateRandomId()),
        moneyReservoirCode = testReservoirCashGbp.code,
        checkDate = date,
        balanceInCents = (balance * 100).toLong,
      )
    )
  }
  private def persistGbpRate(date: LocalDateTime, ratio: Double)(implicit
      entityAccess: FakeJsEntityAccess
  ): Unit = {
    entityAccess.addRemotelyAddedEntities(
      testExchangeRateMeasurementWithId.copy(
        idOption = Some(EntityModification.generateRandomId()),
        date = date,
        foreignCurrencyCode = testReservoirCashGbp.currency.code,
        ratioReferenceToForeignCurrency = ratio,
      )
    )
  }
}

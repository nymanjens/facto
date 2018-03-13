package flux.stores.entries

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import java.time.LocalDate
import java.time.Month._

import common.money.ReferenceMoney
import common.testing.TestObjects._
import common.testing.{FakeJsEntityAccess, TestModule}
import common.time.LocalDateTimes.createDateTime
import common.time.{DatedMonth, LocalDateTime}
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.GainsForMonth
import models.accounting.config.MoneyReservoir
import models.modification.EntityModification
import utest._

import scala2js.Converters._

object SummaryExchangeRateGainsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val exchangeRateManager = testModule.exchangeRateManager
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()
    val factory: SummaryExchangeRateGainsStoreFactory = new SummaryExchangeRateGainsStoreFactory()

    "no transactions" - async {
      val store = factory.get(testAccountA, year = 2013)

      val gainsForYear = await(store.stateFuture)
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2012, DECEMBER, 1))) ==> GainsForMonth.empty
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==> GainsForMonth.empty
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, DECEMBER, 1))) ==> GainsForMonth.empty
    }

    "domestic reservoir" - async {
      persistTransaction(flow = 789, date = createDateTime(2013, JANUARY, 5), reservoir = testReservoirCashA)

      val store = factory.get(testAccountA, year = 2013)
      val gainsForYear = await(store.stateFuture)

      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==> GainsForMonth.empty
    }

    "foreign reservoir" - async {
      persistGbpRate(date = createDateTime(2012, JANUARY, 30), ratio = 1.0)
      persistGbpRate(date = createDateTime(2012, DECEMBER, 30), ratio = 1.1)

      persistGbpRate(date = createDateTime(2013, JANUARY, 2), ratio = 1.0)
      persistGbpRate(date = createDateTime(2013, JANUARY, 30), ratio = 1.2)
      persistGbpRate(date = createDateTime(2013, FEBRUARY, 2), ratio = 1.0)
      persistGbpRate(date = createDateTime(2013, FEBRUARY, 27), ratio = 1.3)
      persistGbpRate(date = createDateTime(2013, MARCH, 2), ratio = 1.2)
      persistGbpRate(date = createDateTime(2013, MARCH, 30), ratio = 1.4)
      persistGbpRate(date = createDateTime(2013, APRIL, 30), ratio = 1.5)
      persistGbpRate(date = createDateTime(2013, MAY, 2), ratio = 1.3)
      persistGbpRate(date = createDateTime(2013, MAY, 6), ratio = 1.7)
      persistGbpRate(date = createDateTime(2013, MAY, 30), ratio = 1.5)
      persistGbpRate(date = createDateTime(2013, JUNE, 30), ratio = 1.6)
      persistGbpRate(date = createDateTime(2013, JULY, 30), ratio = 1.6)
      persistGbpRate(date = createDateTime(2013, AUGUST, 30), ratio = 1.5)

      persistTransaction(flow = 789, date = createDateTime(2012, JANUARY, 5)) // Balance = 789 GBP
      persistBalanceCheck(balance = 100, date = createDateTime(2012, NOVEMBER, 5)) // Balance = 100
      persistTransaction(flow = 100, date = createDateTime(2012, DECEMBER, 5)) // Balance = 200 GBP

      persistTransaction(flow = -400, date = createDateTime(2013, JANUARY, 5)) // Balance = -200 GBP
      persistBalanceCheck(balance = -100, date = createDateTime(2013, FEBRUARY, 5)) // Balance = -100
      persistTransaction(flow = 200, date = createDateTime(2013, MARCH, 5)) // Balance = 100
      persistTransaction(flow = 300, date = createDateTime(2013, MAY, 5)) // Balance = 400
      persistTransaction(flow = -600, date = createDateTime(2013, MAY, 9)) // Balance = -200

      val store = factory.get(testAccountA, year = 2013)
      val gainsForYear = await(store.stateFuture)

      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2012, JANUARY, 1))) ==> GainsForMonth.empty
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2012, NOVEMBER, 1))) ==> GainsForMonth.empty
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2012, DECEMBER, 1))) ==> GainsForMonth.empty

      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, JANUARY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(20 - 80))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, FEBRUARY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-20 + 30))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, MARCH, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-10 + 40))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, APRIL, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(10))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, MAY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(60 + 120))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, JUNE, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(-20))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, JULY, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(0))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, AUGUST, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(20))
      gainsForYear.gainsForMonth(DatedMonth(LocalDate.of(2013, SEPTEMBER, 1))) ==>
        GainsForMonth.forSingle(testReservoirCashGbp, refMoney(0))
    }
  }

  private def refMoney(double: Double): ReferenceMoney = ReferenceMoney((double * 100).toLong)

  private def persistTransaction(
      flow: Double,
      date: LocalDateTime,
      reservoir: MoneyReservoir = testReservoirCashGbp)(implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addRemotelyAddedEntities(
      createTransaction(flow = flow, reservoir = reservoir).copy(transactionDate = date))
  }
  private def persistBalanceCheck(balance: Double, date: LocalDateTime)(
      implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addRemotelyAddedEntities(
      testBalanceCheckWithId.copy(
        idOption = Some(EntityModification.generateRandomId()),
        moneyReservoirCode = testReservoirCashGbp.code,
        checkDate = date,
        balanceInCents = (balance * 100).toLong))
  }
  private def persistGbpRate(date: LocalDateTime, ratio: Double)(
      implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addRemotelyAddedEntities(
      testExchangeRateMeasurementWithId.copy(
        idOption = Some(EntityModification.generateRandomId()),
        date = date,
        foreignCurrencyCode = testReservoirCashGbp.currency.code,
        ratioReferenceToForeignCurrency = ratio
      ))
  }
}

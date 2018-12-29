package app.flux.stores.entries.factories

import java.time.Month
import java.time.Month._

import app.common.money.ReferenceMoney
import app.common.testing.FakeJsEntityAccess
import app.common.testing.TestObjects._
import app.common.time.DatedMonth
import app.flux.stores.entries.ComplexQueryFilter
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import app.models.accounting.Transaction
import app.models.accounting.config.Account
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

object SummaryForYearStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new common.testing.TestModule

    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()
    implicit val exchangeRateManager = testModule.exchangeRateManager
    val factory: SummaryForYearStoreFactory = new SummaryForYearStoreFactory()

    "empty result" - async {
      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear.empty
    }
    "single transaction" - async {
      val transaction = persistTransaction()

      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear(Seq(transaction))
    }
    "multiple transactions" - async {
      val transaction1 = persistTransaction(day = 1, month = JANUARY)
      val transaction2 = persistTransaction(day = 2, month = JANUARY)
      val transaction3 = persistTransaction(day = 3, month = JANUARY)

      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear(Seq(transaction1, transaction2, transaction3))
    }
    "transaction in different year" - async {
      persistTransaction(year = 2010)

      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear.empty
    }

    "transaction for different account" - async {
      persistTransaction(beneficiary = testAccountB)

      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear.empty
    }

    "transaction outside filter" - async {
      persistTransaction(description = "xyz")

      val store = factory.get(testAccountA, 2012, query = "abc")
      val state = await(store.stateFuture)

      state ==> SummaryForYear.empty
    }
    "SummaryForYear" - {
      "months" - {
        val summaryForYear = SummaryForYear(
          Seq(
            createTransaction(year = 2012, month = FEBRUARY),
            createTransaction(year = 2012, month = FEBRUARY),
            createTransaction(year = 2012, month = MARCH)))

        summaryForYear.months ==> Set(DatedMonth.of(2012, FEBRUARY), DatedMonth.of(2012, MARCH))
      }
      "categories" - {
        val summaryForYear = SummaryForYear(
          Seq(
            createTransaction(category = testCategoryA),
            createTransaction(category = testCategoryA),
            createTransaction(category = testCategoryB)))

        summaryForYear.categories ==> Set(testCategoryA, testCategoryB)
      }
      "cell" - {
        val transaction1 = createTransaction(year = 2012, month = JUNE, category = testCategoryA, flow = 12)
        val transaction2 = createTransaction(year = 2012, month = JUNE, category = testCategoryA, flow = -22)
        val transaction3 = createTransaction(year = 2012, month = JULY, category = testCategoryA, flow = -22)
        val transaction4 = createTransaction(year = 2012, month = JUNE, category = testCategoryB, flow = -22)
        val summaryForYear = SummaryForYear(Seq(transaction1, transaction2, transaction3, transaction4))

        val categoryAJuneCell =
          summaryForYear.cell(category = testCategoryA, month = DatedMonth.of(2012, JUNE))
        val emptyCell = summaryForYear.cell(category = testCategoryB, month = DatedMonth.of(2012, JULY))

        "transactions" - {
          categoryAJuneCell.transactions ==> Seq(transaction1, transaction2)
          emptyCell.transactions ==> Seq()
        }
        "nonEmpty" - {
          categoryAJuneCell.nonEmpty ==> true
          emptyCell.nonEmpty ==> false
        }
        "totalFlow" - {
          categoryAJuneCell.totalFlow ==> ReferenceMoney(-1000)
          emptyCell.totalFlow ==> ReferenceMoney(0)
        }
      }
    }
  }

  private def persistTransaction(
      year: Int = 2012,
      month: Month = MARCH,
      day: Int = 20,
      beneficiary: Account = testAccountA,
      description: String = "abcdefg")(implicit entityAccess: FakeJsEntityAccess): Transaction = {
    val transaction =
      createTransaction(
        beneficiary = beneficiary,
        description = description,
        year = year,
        month = month,
        day = day)
    entityAccess.addRemotelyAddedEntities(transaction)
    transaction
  }
}

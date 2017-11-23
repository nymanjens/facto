package flux.stores.entries

import java.time.Month
import java.time.Month._

import common.testing.FakeRemoteDatabaseProxy
import common.testing.TestObjects._
import common.time.DatedMonth
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import models.accounting.Transaction
import models.accounting.config.Account
import models.accounting.money.ReferenceMoney
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object SummaryForYearStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new common.testing.TestModule

    implicit val database = testModule.fakeRemoteDatabaseProxy
    implicit val userManager = testModule.entityAccess.userManager
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()
    implicit val exchangeRateManager = testModule.exchangeRateManager
    val factory: SummaryForYearStoreFactory = new SummaryForYearStoreFactory()

    "empty result" - {
      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear.empty
    }

    "single transaction" - {
      val transaction = persistTransaction()

      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear(Seq(transaction))
    }
    "multiple transactions" - {
      val transaction1 = persistTransaction(day = 1, month = JANUARY)
      val transaction2 = persistTransaction(day = 2, month = JANUARY)
      val transaction3 = persistTransaction(day = 3, month = JANUARY)

      factory.get(testAccountA, 2012, query = "abc").state ==>
        SummaryForYear(Seq(transaction1, transaction2, transaction3))
    }
    "transaction in different year" - {
      persistTransaction(year = 2010)

      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear.empty
    }

    "transaction for different account" - {
      persistTransaction(beneficiary = testAccountB)

      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear.empty
    }

    "transaction outside filter" - {
      persistTransaction(description = "xyz")

      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear.empty
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
      description: String = "abcdefg")(implicit database: FakeRemoteDatabaseProxy): Transaction = {
    val transaction =
      createTransaction(
        beneficiary = beneficiary,
        description = description,
        year = year,
        month = month,
        day = day)
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
}

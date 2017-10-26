package flux.stores.entries

import java.time.Month

import common.testing.FakeRemoteDatabaseProxy
import common.testing.TestObjects._
import common.time.{LocalDateTimes, YearRange}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import models.accounting.Transaction
import models.accounting.config.Account
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
    val factory: SummaryForYearStoreFactory = new SummaryForYearStoreFactory()

    "empty result" - {
      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear.empty
    }

    "single transaction" - {
      val transaction = persistTransaction()

      factory.get(testAccountA, 2012, query = "abc").state ==> SummaryForYear(Seq(transaction))
    }
    "multiple transactions" - {
      val transaction1 = persistTransaction(day = 1)
      val transaction2 = persistTransaction(day = 2)
      val transaction3 = persistTransaction(day = 3)

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
  }

  private def persistTransaction(
      year: Int = 2012,
      day: Int = 20,
      beneficiary: Account = testAccountA,
      description: String = "abcdefg")(implicit database: FakeRemoteDatabaseProxy): Transaction = {
    val transaction =
      createTransaction(beneficiary = beneficiary, description = description, year = year, day = day)
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
}

package flux.stores.entries

import common.testing.TestObjects._
import common.testing.{FakeJsEntityAccess, TestModule}
import common.time.YearRange
import models.accounting.config.Account
import utest._

import scala2js.Converters._

object SummaryYearsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    val factory: SummaryYearsStoreFactory = new SummaryYearsStoreFactory()

    "empty result" - {
      factory.get(testAccountA).state.yearRange ==> YearRange.empty
    }

    "single transaction" - {
      persistTransaction(2012)

      factory.get(testAccountA).state.yearRange ==> YearRange.single(2012)
    }
    "transactions in multiple years" - {
      persistTransaction(2010)
      persistTransaction(2012)
      persistTransaction(2015)
      persistTransaction(2018)
      persistTransaction(2018)

      factory.get(testAccountA).state.yearRange ==> YearRange.closed(2010, 2018)
    }
    "transaction for different account" - {
      persistTransaction(2012, beneficiary = testAccountB)

      factory.get(testAccountA).state.yearRange ==> YearRange.empty
    }
  }

  private def persistTransaction(year: Int, beneficiary: Account = testAccountA)(
      implicit database: FakeJsEntityAccess): Unit = {
    database.addRemotelyAddedEntities(createTransaction(beneficiary = beneficiary, year = year))
  }
}

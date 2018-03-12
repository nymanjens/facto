package flux.stores.entries

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import common.testing.TestObjects._
import common.testing.{FakeJsEntityAccess, TestModule}
import common.time.YearRange
import models.accounting.config.Account
import utest._

import scala2js.Converters._

object SummaryYearsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeEntityAccess
    val factory: SummaryYearsStoreFactory = new SummaryYearsStoreFactory()

    "empty result" - async {
      val state = await(factory.get(testAccountA).stateFuture)

      state.yearRange ==> YearRange.empty
    }

    "single transaction" - async {
      persistTransaction(2012)

      val state = await(factory.get(testAccountA).stateFuture)

      state.yearRange ==> YearRange.single(2012)
    }
    "transactions in multiple years" - async {
      persistTransaction(2010)
      persistTransaction(2012)
      persistTransaction(2015)
      persistTransaction(2018)
      persistTransaction(2018)

      val state = await(factory.get(testAccountA).stateFuture)

      state.yearRange ==> YearRange.closed(2010, 2018)
    }
    "transaction for different account" - async {
      persistTransaction(2012, beneficiary = testAccountB)

      val state = await(factory.get(testAccountA).stateFuture)

      state.yearRange ==> YearRange.empty
    }
  }

  private def persistTransaction(year: Int, beneficiary: Account = testAccountA)(
      implicit database: FakeJsEntityAccess): Unit = {
    database.addRemotelyAddedEntities(createTransaction(beneficiary = beneficiary, year = year))
  }
}

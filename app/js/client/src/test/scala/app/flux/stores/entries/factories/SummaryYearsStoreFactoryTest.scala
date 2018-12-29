package app.flux.stores.entries.factories

import app.common.testing.TestObjects._
import app.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import app.common.time.YearRange
import app.models.accounting.config.Account
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

object SummaryYearsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val entityAccess = testModule.fakeEntityAccess
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
      implicit entityAccess: FakeJsEntityAccess): Unit = {
    entityAccess.addRemotelyAddedEntities(createTransaction(beneficiary = beneficiary, year = year))
  }
}

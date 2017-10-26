package flux.stores.entries

import java.time.Month
import java.time.Month.JANUARY

import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.testing.TestObjects._
import common.time.{LocalDateTime, LocalDateTimes, YearRange}
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.accounting.config.{Account, MoneyReservoir}
import models.accounting.money.ReferenceMoney
import models.manager.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object SummaryYearsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    val factory: SummaryYearsStoreFactory = new SummaryYearsStoreFactory()

    "empty result" - {
      factory.get(testAccountA).state ==> YearRange.empty
    }

    "single transaction" - {
      persistTransaction(2012)

      factory.get(testAccountA).state ==> YearRange.single(2012)
    }
    "transactions in multiple years" - {
      persistTransaction(2010)
      persistTransaction(2012)
      persistTransaction(2015)
      persistTransaction(2018)
      persistTransaction(2018)

      factory.get(testAccountA).state ==> YearRange.closed(2010, 2018)
    }
    "transaction for different account" - {
      persistTransaction(2012, beneficiary = testAccountB)

      factory.get(testAccountA).state ==> YearRange.empty
    }
  }

  private def persistTransaction(year: Int, beneficiary: Account = testAccountA)(
      implicit database: FakeRemoteDatabaseProxy): Unit = {
    database.addRemotelyAddedEntities(createTransaction(beneficiary = beneficiary, year = year))
  }
}

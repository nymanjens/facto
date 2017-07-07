package flux.stores.entries

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.time.LocalDateTimes.createDateTime
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.config.{Account, Category}
import utest._

import scala.collection.immutable.Seq
import scala.util.Random
import scala2js.Converters._

object EndowmentEntriesStoreFactoryTest extends TestSuite {
  implicit private val accountingConfig = new TestModule().accountingConfig

  override def tests = TestSuite {
    implicit val database = new FakeRemoteDatabaseProxy()
    val factory: EndowmentEntriesStoreFactory = new EndowmentEntriesStoreFactory()

    val trans1 = persistTransaction(id = 1, consumedDay = 1, account = testAccountA)
    val trans2 = persistTransaction(id = 2, consumedDay = 2, account = testAccountA)
    val trans3 = persistTransaction(id = 3, consumedDay = 3, createdDay = 1, account = testAccountA)
    val trans4 = persistTransaction(id = 4, consumedDay = 3, createdDay = 2, account = testAccountA)
    persistTransaction(id = 5, consumedDay = 3, account = testAccountB)
    persistTransaction(id = 6, consumedDay = 3, category = testCategory)

    "filters and sorts entries correctly" - {
      val store = factory.get(testAccountA, maxNumEntries = 5)

      store.state ==> EntriesListStoreFactory.State
        .withGeneralEntries(hasMore = false, Seq(trans1), Seq(trans2), Seq(trans3), Seq(trans4))
    }

    "respects maxNumEntries" - {
      val store = factory.get(testAccountA, maxNumEntries = 3)

      store.state ==> EntriesListStoreFactory.State
        .withGeneralEntries(hasMore = true, Seq(trans2), Seq(trans3), Seq(trans4))
    }
  }

  private def persistTransaction(id: Long,
                                 consumedDay: Int,
                                 createdDay: Int = 1,
                                 account: Account = testAccountA,
                                 category: Category = accountingConfig.constants.endowmentCategory)(
      implicit database: FakeRemoteDatabaseProxy): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(id),
      transactionGroupId = id,
      flowInCents = new Random().nextLong(),
      createdDate = createDateTime(2012, JANUARY, createdDay),
      transactionDate = createDateTime(2012, JANUARY, createdDay),
      consumedDate = createDateTime(2012, JANUARY, consumedDay),
      beneficiaryAccountCode = account.code,
      categoryCode = category.code
    )
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
}

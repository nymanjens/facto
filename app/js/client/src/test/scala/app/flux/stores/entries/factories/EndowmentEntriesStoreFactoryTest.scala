package app.flux.stores.entries.factories

import java.time.Month.JANUARY

import app.common.testing.FakeAppJsEntityAccess
import hydro.common.testing.FakeJsEntityAccess
import app.common.testing.TestObjects._
import app.flux.stores.entries.GeneralEntry.toGeneralEntrySeq
import app.models.accounting._
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import hydro.common.time.LocalDateTimes.createDateTime
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Random

object EndowmentEntriesStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    implicit val entityAccess: FakeAppJsEntityAccess = new FakeAppJsEntityAccess()
    val factory: EndowmentEntriesStoreFactory = new EndowmentEntriesStoreFactory()

    val trans1 = persistTransaction(id = 1, consumedDay = 1, account = testAccountA)
    val trans2 = persistTransaction(id = 2, consumedDay = 2, account = testAccountA)
    val trans3 = persistTransaction(id = 3, consumedDay = 3, createdDay = 1, account = testAccountA)
    val trans4 = persistTransaction(id = 4, consumedDay = 3, createdDay = 2, account = testAccountA)
    persistTransaction(id = 5, consumedDay = 3, account = testAccountB)
    persistTransaction(id = 6, consumedDay = 3, category = testCategory)

    "filters and sorts entries correctly" - async {
      val store = factory.get(testAccountA, maxNumEntries = 5)
      val state = await(store.stateFuture)

      state.hasMore ==> false
      state.entries.map(_.entry) ==> toGeneralEntrySeq(Seq(trans1), Seq(trans2), Seq(trans3), Seq(trans4))
    }

    "respects maxNumEntries" - async {
      val store = factory.get(testAccountA, maxNumEntries = 3)
      val state = await(store.stateFuture)

      state.hasMore ==> true
      state.entries.map(_.entry) ==> toGeneralEntrySeq(Seq(trans2), Seq(trans3), Seq(trans4))
    }
  }

  private def persistTransaction(
      id: Long,
      consumedDay: Int,
      createdDay: Int = 1,
      account: Account = testAccountA,
      category: Category = testAccountingConfig.constants.endowmentCategory,
  )(implicit entityAccess: FakeJsEntityAccess): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(id),
      transactionGroupId = id,
      flowInCents = new Random().nextLong(),
      createdDate = createDateTime(2012, JANUARY, createdDay),
      transactionDate = createDateTime(2012, JANUARY, createdDay),
      consumedDate = createDateTime(2012, JANUARY, consumedDay),
      beneficiaryAccountCode = account.code,
      categoryCode = category.code,
    )
    entityAccess.addRemotelyAddedEntities(transaction)
    transaction
  }
}

package app.flux.stores.entries.factories

import app.common.accounting.ComplexQueryFilter
import app.common.testing.TestObjects._
import app.flux.stores.entries.GeneralEntry.toGeneralEntrySeq
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ComplexQueryStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new app.common.testing.TestModule

    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()

    val factory: ComplexQueryStoreFactory = new ComplexQueryStoreFactory()

    val trans1 = createTransaction(id = 1, groupId = 1, day = 1, description = "cats and dogs")
    val trans2 = createTransaction(id = 2, groupId = 1, day = 1, description = "docs and snakes")
    val trans3 = createTransaction(id = 3, groupId = 1, day = 1, description = "snakes and cats")
    val trans4 = createTransaction(id = 4, groupId = 2, day = 2, description = "cats")
    val trans5 = createTransaction(id = 5, groupId = 2, day = 2, description = "cats cats")
    entityAccess.addRemotelyAddedEntities(trans1, trans2, trans3, trans4, trans5)

    "filters and sorts entries correctly" - async {
      val store = factory.get("cats", maxNumEntries = 2)
      val state = await(store.stateFuture)

      state.hasMore ==> false
      state.entries.map(_.entry) ==> toGeneralEntrySeq(Seq(trans1, trans3), Seq(trans4, trans5))
    }

    "respects maxNumEntries" - async {
      val store = factory.get("cats", maxNumEntries = 1)
      val state = await(store.stateFuture)

      state.hasMore ==> true
      state.entries.map(_.entry) ==> toGeneralEntrySeq(Seq(trans4, trans5))
    }
  }
}

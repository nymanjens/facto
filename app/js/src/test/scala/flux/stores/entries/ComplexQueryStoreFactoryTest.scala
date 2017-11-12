package flux.stores.entries

import common.testing.TestObjects._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object ComplexQueryStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new common.testing.TestModule

    implicit val database = testModule.fakeRemoteDatabaseProxy
    implicit val userManager = testModule.entityAccess.userManager
    implicit val testAccountingConfig = testModule.testAccountingConfig
    implicit val complexQueryFilter = new ComplexQueryFilter()

    val factory: ComplexQueryStoreFactory = new ComplexQueryStoreFactory()

    val trans1 = createTransaction(id = 1, groupId = 1, day = 1, description = "cats and dogs")
    val trans2 = createTransaction(id = 2, groupId = 1, day = 1, description = "docs and snakes")
    val trans3 = createTransaction(id = 3, groupId = 1, day = 1, description = "snakes and cats")
    val trans4 = createTransaction(id = 4, groupId = 2, day = 2, description = "cats")
    val trans5 = createTransaction(id = 5, groupId = 2, day = 2, description = "cats cats")
    database.addRemotelyAddedEntities(trans1, trans2, trans3, trans4, trans5)

    "filters and sorts entries correctly" - {
      val store = factory.get("cats", maxNumEntries = 2)

      store.state.hasMore ==> false
      store.state.entries ==> GeneralEntry.toGeneralEntrySeq(Seq(trans1, trans3), Seq(trans4, trans5))
    }

    "respects maxNumEntries" - {
      val store = factory.get("cats", maxNumEntries = 1)

      store.state.hasMore ==> true
      store.state.entries ==> GeneralEntry.toGeneralEntrySeq(Seq(trans4, trans5))
    }
  }
}

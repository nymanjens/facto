package flux.stores

import java.time.Month.JANUARY

import common.testing.FakeRemoteDatabaseProxy
import common.testing.TestObjects._
import common.time.LocalDateTime
import common.time.LocalDateTimes.createDateTime
import flux.stores.entries.GeneralEntry
import models.accounting._
import models.manager.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

/** Test test also tests `EntriesStoreFactory` and `EntriesStore`. */
object LastNEntriesStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    implicit val database = new FakeRemoteDatabaseProxy()
    val factory: LastNEntriesStoreFactory = new LastNEntriesStoreFactory()
    val store: factory.Store = factory.get(maxNumEntries = 3)

    "factory result is cached" - {
      factory.get(maxNumEntries = 3) ==> store
      assert(factory.get(maxNumEntries = 4) != store)
    }

    "store state is updated upon remote update" - {
      store.state ==> EntriesStoreListFactory.State.empty

      database.addRemotelyAddedEntities(testTransactionWithId)

      store.state ==> EntriesStoreListFactory.State(Seq(GeneralEntry(Seq(testTransactionWithId))), hasMore = false)
    }

    "store state is updated upon local update" - {
      store.state ==> EntriesStoreListFactory.State.empty

      database.persistModifications(Seq(EntityModification.Add(testTransactionWithId)))

      store.state ==> EntriesStoreListFactory.State(Seq(GeneralEntry(Seq(testTransactionWithId))), hasMore = false)
    }

    "store calls listeners" - {
      var onStateUpdateCount = 0
      store.register(new EntriesStore.Listener {
        override def onStateUpdate() = {
          onStateUpdateCount += 1
          store.state // get state so the store knows of our interest
        }
      })
      store.state // get state so the store knows of our interest

      database.persistModifications(Seq(EntityModification.Add(testTransactionWithIdB)))

      onStateUpdateCount ==> 2 // Once for local modification, once for remote persistence

      database.addRemotelyAddedEntities(testTransactionWithIdA)

      onStateUpdateCount ==> 3
    }

    "combines consecutive transactions" - {
      val trans1 = testTransactionWithIdA.copy(idOption = Some(11), transactionGroupId = 122, createdDate = createDateTime(2012, JANUARY, 1))
      val trans2 = testTransactionWithIdA.copy(idOption = Some(22), transactionGroupId = 122, createdDate = createDateTime(2012, JANUARY, 2))
      val trans3 = testTransactionWithIdA.copy(idOption = Some(33), transactionGroupId = 133, createdDate = createDateTime(2012, JANUARY, 3))

      database.addRemotelyAddedEntities(trans1, trans2, trans3)

      store.state ==> EntriesStoreListFactory.State(Seq(
        GeneralEntry(Seq(trans1, trans2)),
        GeneralEntry(Seq(trans3))),
        hasMore = false)
    }

    "sorts entries on transaction date first and then created date" - {
      def transaction(id: Long, transactionDate: LocalDateTime, createdDate: LocalDateTime): Transaction = {
        testTransactionWithIdA.copy(idOption = Some(id), transactionGroupId = id, createdDate = createdDate, transactionDate = transactionDate)
      }
      val trans1 = transaction(111, transactionDate = createDateTime(2012, JANUARY, 2), createdDate = createDateTime(2012, JANUARY, 3))
      val trans2 = transaction(222, transactionDate = createDateTime(2012, JANUARY, 3), createdDate = createDateTime(2012, JANUARY, 1))
      val trans3 = transaction(333, transactionDate = createDateTime(2012, JANUARY, 3), createdDate = createDateTime(2012, JANUARY, 2))

      database.addRemotelyAddedEntities(trans3, trans2, trans1)

      store.state ==> EntriesStoreListFactory.State(Seq(
        GeneralEntry(Seq(trans1)),
        GeneralEntry(Seq(trans2)),
        GeneralEntry(Seq(trans3))),
        hasMore = false)
    }

    "respects maximum N" - {
      def transaction(id: Long, date: LocalDateTime): Transaction = {
        testTransactionWithIdA.copy(idOption = Some(id), transactionGroupId = id, createdDate = date)
      }
      val trans1 = transaction(111, createDateTime(2012, JANUARY, 1))
      val trans2 = transaction(222, createDateTime(2012, JANUARY, 2))
      val trans3 = transaction(333, createDateTime(2012, JANUARY, 3))
      val trans4 = transaction(444, createDateTime(2012, JANUARY, 4))

      database.addRemotelyAddedEntities(trans1, trans2, trans3, trans4)

      store.state ==> EntriesStoreListFactory.State(Seq(
        GeneralEntry(Seq(trans2)),
        GeneralEntry(Seq(trans3)),
        GeneralEntry(Seq(trans4))),
        hasMore = true)
    }
  }
}

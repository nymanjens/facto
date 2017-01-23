package stores

import java.time.Month.JANUARY
import java.lang.Math.abs
import common.testing.FakeRemoteDatabaseProxy
import common.time.{LocalDateTime, LocalDateTimes}
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{EntityModification, EntityType}
import utest._
import common.testing.TestObjects._
import models.User
import models.access.RemoteDatabaseProxy
import stores.LastNEntriesStoreFactory.LastNEntriesState
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.util.Random
import scala2js.Converters._

/** Test test also tests `EntriesStoreFactory` and `EntriesStore`. */
object LastNEntriesStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    implicit val database = new FakeRemoteDatabaseProxy()
    val factory: LastNEntriesStoreFactory = new LastNEntriesStoreFactory()
    val store: EntriesStore[LastNEntriesState] = factory.get(3 /* n */)

    "factory result is cached" - {
      factory.get(3 /* n */) ==> store
      assert(factory.get(4 /* n */) != store)
    }

    "store state is updated upon remote update" - {
      store.state ==> LastNEntriesState(Seq())

      database.addRemotelyAddedEntities(testTransactionWithId)

      store.state ==> LastNEntriesState(Seq(GeneralEntry(Seq(testTransactionWithId))))
    }

    "store state is updated upon local update" - {
      store.state ==> LastNEntriesState(Seq())

      database.persistModifications(Seq(EntityModification.Add(testTransactionWithId)))

      store.state ==> LastNEntriesState(Seq(GeneralEntry(Seq(testTransactionWithId))))
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

      store.state ==> LastNEntriesState(Seq(
        GeneralEntry(Seq(trans1, trans2)),
        GeneralEntry(Seq(trans3))))
    }

    "sorts entries on transaction date first and then created date" - {
      def transaction(id: Long, transactionDate: LocalDateTime, createdDate: LocalDateTime): Transaction = {
        testTransactionWithIdA.copy(idOption = Some(id), transactionGroupId = id, createdDate = createdDate, transactionDate = transactionDate)
      }
      val trans1 = transaction(111, transactionDate = createDateTime(2012, JANUARY, 2), createdDate = createDateTime(2012, JANUARY, 3))
      val trans2 = transaction(222, transactionDate = createDateTime(2012, JANUARY, 3), createdDate = createDateTime(2012, JANUARY, 1))
      val trans3 = transaction(333, transactionDate = createDateTime(2012, JANUARY, 3), createdDate = createDateTime(2012, JANUARY, 2))

      database.addRemotelyAddedEntities(trans3, trans2, trans1)

      store.state ==> LastNEntriesState(Seq(
        GeneralEntry(Seq(trans1)),
        GeneralEntry(Seq(trans2)),
        GeneralEntry(Seq(trans3))))
    }

    "respects maximum (n)" - {
      def transaction(id: Long, date: LocalDateTime): Transaction = {
        testTransactionWithIdA.copy(idOption = Some(id), transactionGroupId = id, createdDate = date)
      }
      val trans1 = transaction(111, createDateTime(2012, JANUARY, 1))
      val trans2 = transaction(222, createDateTime(2012, JANUARY, 2))
      val trans3 = transaction(333, createDateTime(2012, JANUARY, 3))
      val trans4 = transaction(444, createDateTime(2012, JANUARY, 4))

      database.addRemotelyAddedEntities(trans1, trans2, trans3, trans4)

      store.state ==> LastNEntriesState(Seq(
        GeneralEntry(Seq(trans2)),
        GeneralEntry(Seq(trans3)),
        GeneralEntry(Seq(trans4))))
    }
  }
}

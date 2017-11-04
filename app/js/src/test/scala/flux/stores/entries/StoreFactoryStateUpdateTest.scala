package flux.stores.entries

import java.time.Month.JANUARY

import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.testing.TestObjects._
import common.time.LocalDateTime
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.manager.EntityModification
import utest._

import scala.collection.immutable.{ListMap, Seq}
import scala2js.Converters._

object StoreFactoryStateUpdateTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    val database = testModule.fakeRemoteDatabaseProxy

    def runTest(store: EntriesStore[_], updatesWithImpact: ListMap[EntityModification, StateImpact]): Unit = {
      var lastState = store.state

      for ((update, stateImpact) <- updatesWithImpact) {
        database.persistModifications(update)

        stateImpact match {
          case StateImpact.NoChange =>
            store.state ==> lastState
          case StateImpact.Change =>
            val storeState = store.state
            assert(storeState != lastState)
          case StateImpact.Undefined =>
        }

        lastState = store.state
      }
    }

    "AllEntriesStoreFactory" - runTest(
      store = testModule.allEntriesStoreFactory.get(maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        EntityModification.Add(createTransaction(day = 10, id = 10)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 9, id = 9)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 8, id = 8)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 7, id = 7)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 6, id = 6)) -> StateImpact.NoChange,
        // Remove Transactions
        EntityModification.Remove[Transaction](6) -> StateImpact.NoChange,
        EntityModification.Remove[Transaction](10) -> StateImpact.Change,
        // Add BalanceChecks
        EntityModification.Add(createBalanceCheck(id = 91)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        EntityModification.Remove[BalanceCheck](91) -> StateImpact.NoChange
      )
    )

    "CashFlowEntriesStoreFactory" - runTest(
      store =
        testModule.cashFlowEntriesStoreFactory.get(moneyReservoir = testReservoirCardA, maxNumEntries = 2),
      updatesWithImpact = ListMap(
        // Add Transactions and BalanceChecks
        EntityModification
          .Add(createTransaction(day = 10, id = 10, reservoir = testReservoirCardA)) -> StateImpact.Change,
        EntityModification
          .Add(createTransaction(day = 9, id = 9, reservoir = testReservoirCardA)) -> StateImpact.Change,
        EntityModification
          .Add(createTransaction(day = 8, id = 8, reservoir = testReservoirCardA)) -> StateImpact.Change,
        EntityModification
          .Add(createTransaction(day = 7, id = 7, reservoir = testReservoirCardB)) -> StateImpact.Change,
        EntityModification
          .Add(createBalanceCheck(day = 6, id = 96, reservoir = testReservoirCardB)) -> StateImpact.Change,
        EntityModification
          .Add(createTransaction(day = 5, id = 5, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        EntityModification
          .Add(createBalanceCheck(day = 4, id = 94, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        // Remove Transactions and BalanceChecks
        EntityModification.Remove[Transaction](5) -> StateImpact.NoChange,
        EntityModification.Remove[Transaction](6) -> StateImpact.Change,
        EntityModification.Remove[Transaction](4) -> StateImpact.Change
      )
    )
  }

  private sealed trait StateImpact
  private object StateImpact {
    object NoChange extends StateImpact
    object Change extends StateImpact
    object Undefined extends StateImpact
  }

  private final class ThisTestModule extends TestModule {

    import com.softwaremill.macwire._

    implicit val allEntriesStoreFactory: AllEntriesStoreFactory = wire[AllEntriesStoreFactory]
    implicit val cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory = wire[CashFlowEntriesStoreFactory]
  }
}

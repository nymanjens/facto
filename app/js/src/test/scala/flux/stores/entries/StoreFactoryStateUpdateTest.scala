package flux.stores.entries

import java.time.Month.JANUARY

import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.testing.TestObjects._
import common.time.LocalDateTime
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.manager.EntityModification
import models.manager.EntityModification._
import utest._

import scala.collection.immutable.{ListMap, Seq}
import scala2js.Converters._

object StoreFactoryStateUpdateTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy

    "AllEntriesStoreFactory" - runTest(
      store = testModule.allEntriesStoreFactory.get(maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(day = 10, id = 10)) -> StateImpact.Change,
        Add(createTransaction(day = 9, id = 9)) -> StateImpact.Change,
        Add(createTransaction(day = 8, id = 8)) -> StateImpact.Change,
        Add(createTransaction(day = 7, id = 7)) -> StateImpact.Change,
        Add(createTransaction(day = 6, id = 6)) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](6) -> StateImpact.NoChange,
        Remove[Transaction](10) -> StateImpact.Change,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 91)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](91) -> StateImpact.NoChange
      )
    )

    "CashFlowEntriesStoreFactory" - runTest(
      store =
        testModule.cashFlowEntriesStoreFactory.get(moneyReservoir = testReservoirCardA, maxNumEntries = 2),
      updatesWithImpact = ListMap(
        // Add Transactions and BalanceChecks
        Add(createTransaction(day = 10, id = 10, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(day = 9, id = 9, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(day = 8, id = 8, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(day = 7, id = 7, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createBalanceCheck(day = 6, id = 6, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(day = 5, id = 5, reservoir = testReservoirCardA)) -> StateImpact.NoChange,
        Add(createBalanceCheck(day = 4, id = 4, reservoir = testReservoirCardA)) -> StateImpact.NoChange,
        // Adding irrelevant Transactions and BalanceChecks
        Add(createTransaction(day = 11, id = 11, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        Add(createBalanceCheck(day = 12, id = 12, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        // Remove Transactions and BalanceChecks
        Remove[Transaction](5) -> StateImpact.NoChange,
        Remove[BalanceCheck](6) -> StateImpact.Change,
        Remove[BalanceCheck](4) -> StateImpact.Change,
        // Removing irrelevant Transactions and BalanceChecks
        Remove[Transaction](11) -> StateImpact.NoChange,
        Remove[BalanceCheck](12) -> StateImpact.NoChange
      )
    )
  }

  private def runTest(store: EntriesStore[_], updatesWithImpact: ListMap[EntityModification, StateImpact])(
      implicit database: FakeRemoteDatabaseProxy): Unit = {
    var lastState = store.state

    for ((update, stateImpact) <- updatesWithImpact) {
      database.persistModifications(update)

      stateImpact match {
        case StateImpact.NoChange =>
          Predef.assert(
            removeImpactingIds(store.state) == removeImpactingIds(lastState),
            s"For update $update:\n" +
              s"Expected states to be the same (ignoring impacting IDs).\n" +
              s"Previous: ${lastState}\n" +
              s"Current:  ${store.state}\n"
          )
        case StateImpact.Change =>
          Predef.assert(
            removeImpactingIds(store.state) != removeImpactingIds(lastState),
            s"For update $update:\n" +
              s"Expected states to be different (ignoring impacting IDs).\n" +
              s"Previous: ${lastState}\n" +
              s"Current:  ${store.state}\n"
          )
        case StateImpact.Undefined =>
      }

      lastState = store.state
    }
  }

  private def removeImpactingIds(state: Any): EntriesStore.StateTrait = {
    state match {
      case s: EntriesListStoreFactory.State[_] =>
        s.copy(impactingTransactionIds = Set(), impactingBalanceCheckIds = Set())
    }
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

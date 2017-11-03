package flux.stores.entries

import java.time.Month.JANUARY

import common.testing.FakeRemoteDatabaseProxy
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
    implicit val database = new FakeRemoteDatabaseProxy()
    val factory: AllEntriesStoreFactory = new AllEntriesStoreFactory()

    def runTest(store: EntriesStore[_], updatesWithImpact: ListMap[EntityModification, StateImpact]): Unit = {
      var lastState = store.state

      for ((update, stateImpact) <- updatesWithImpact) {
        database.persistModifications(update)

        stateImpact match {
          case StateImpact.NoChange => store.state ==> lastState
          case StateImpact.Change => assert(store.state != lastState)
          case StateImpact.Undefined =>
        }

        lastState = store.state
      }
    }

    "AllEntriesStoreFactory" - runTest(
      store = factory.get(maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        EntityModification.Add(createTransaction(day = 10, id = 10)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 9, id = 9)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 8, id = 8)) -> StateImpact.Change,
        EntityModification.Add(createTransaction(day = 7, id = 7)) -> StateImpact.Undefined,
        EntityModification.Add(createTransaction(day = 6, id = 6)) -> StateImpact.Undefined,
        // Remove Transactions
        EntityModification.Remove[Transaction](6) -> StateImpact.NoChange,
        EntityModification.Remove[Transaction](10) -> StateImpact.Change,
        // Add BalanceChecks
        EntityModification.Add(createBalanceCheck(id = 91)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        EntityModification.Remove[BalanceCheck](91) -> StateImpact.NoChange
      )
    )
  }

  private sealed trait StateImpact
  private object StateImpact {
    object NoChange extends StateImpact
    object Change extends StateImpact
    object Undefined extends StateImpact
  }
}

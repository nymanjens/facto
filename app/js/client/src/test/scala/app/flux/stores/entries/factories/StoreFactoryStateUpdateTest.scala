package app.flux.stores.entries.factories

import java.time.Month._

import app.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.flux.stores.entries.AccountPair
import app.flux.stores.entries.ComplexQueryFilter
import app.flux.stores.entries.EntriesStore
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.GainsForYear
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import app.models.access.ModelFields
import app.models.accounting._
import app.models.modification.EntityModification._
import app.models.modification._
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object StoreFactoryStateUpdateTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val entityAccess = testModule.fakeEntityAccess

    "AllEntriesStoreFactory" - runTest(
      store = testModule.allEntriesStoreFactory.get(maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, day = 10)) -> StateImpact.Change,
        Add(createTransaction(id = 9, day = 9)) -> StateImpact.Change,
        Add(createTransaction(id = 8, day = 8)) -> StateImpact.Change,
        Add(createTransaction(id = 7, day = 7)) -> StateImpact.Change,
        Add(createTransaction(id = 6, day = 6)) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](6) -> StateImpact.NoChange,
        Remove[Transaction](10) -> StateImpact.Change,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "CashFlowEntriesStoreFactory" - runTest(
      store =
        testModule.cashFlowEntriesStoreFactory.get(moneyReservoir = testReservoirCardA, maxNumEntries = 2),
      updatesWithImpact = ListMap(
        // Add Transactions and BalanceChecks
        Add(createTransaction(id = 10, day = 10, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(id = 9, day = 9, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(id = 8, day = 8, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(id = 7, day = 7, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createBalanceCheck(id = 6, day = 6, reservoir = testReservoirCardA)) -> StateImpact.Change,
        Add(createTransaction(id = 5, day = 5, reservoir = testReservoirCardA)) -> StateImpact.NoChange,
        Add(createBalanceCheck(id = 4, day = 4, reservoir = testReservoirCardA)) -> StateImpact.NoChange,
        // Adding irrelevant Transactions and BalanceChecks
        Add(createTransaction(id = 11, day = 11, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        Add(createBalanceCheck(id = 12, day = 12, reservoir = testReservoirCardB)) -> StateImpact.NoChange,
        // Remove Transactions and BalanceChecks
        Remove[Transaction](5) -> StateImpact.NoChange,
        Remove[BalanceCheck](6) -> StateImpact.Change,
        Remove[BalanceCheck](4) -> StateImpact.Change,
        // Removing irrelevant Transactions and BalanceChecks
        Remove[Transaction](11) -> StateImpact.NoChange,
        Remove[BalanceCheck](12) -> StateImpact.NoChange
      )
    )

    "ComplexQueryStoreFactory" - runTest(
      store = testModule.complexQueryStoreFactory.get(query = "ABCD", maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, description = "ABCDE")) -> StateImpact.Change,
        Add(createTransaction(id = 9, description = "XXYZ")) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](10) -> StateImpact.Change,
        Remove[Transaction](9) -> StateImpact.NoChange,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "EndowmentEntriesStoreFactory" - runTest(
      store = testModule.endowmentEntriesStoreFactory.get(account = testAccountA, maxNumEntries = 3),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(
          createTransaction(id = 10, beneficiary = testAccountA, category = testConstants.endowmentCategory))
          -> StateImpact.Change,
        Add(createTransaction(id = 9, beneficiary = testAccountA)) -> StateImpact.NoChange,
        Add(createTransaction(id = 8, beneficiary = testAccountB, category = testConstants.endowmentCategory))
          -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](10) -> StateImpact.Change,
        Remove[Transaction](9) -> StateImpact.NoChange,
        Remove[Transaction](8) -> StateImpact.NoChange,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "LiquidationEntriesStoreFactory" - runTest(
      store = testModule.liquidationEntriesStoreFactory
        .get(accountPair = AccountPair(testAccountA, testAccountB), maxNumEntries = 2),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, beneficiary = testAccountA, reservoir = testReservoirCardB)) -> StateImpact.Change,
        Add(createTransaction(id = 9, beneficiary = testAccountA, reservoir = testReservoirCardA)) -> StateImpact.NoChange,
        Add(createTransaction(id = 8, beneficiary = testAccountA, reservoir = testReservoirCardB)) -> StateImpact.Change,
        Add(createTransaction(id = 7, beneficiary = testAccountA, reservoir = testReservoirCardB)) -> StateImpact.Change,
        Add(createTransaction(id = 6, beneficiary = testAccountA, reservoir = testReservoirCardB)) -> StateImpact.Change,
        Add(createTransaction(id = 5, beneficiary = testAccountA, reservoir = testReservoirCardB)) -> StateImpact.Change,
        // Remove Transactions
        Remove[Transaction](5) -> StateImpact.Change,
        Remove[Transaction](9) -> StateImpact.NoChange,
        Remove[Transaction](10) -> StateImpact.Change,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "SummaryExchangeRateGainsStoreFactory" - runTest(
      store = testModule.summaryExchangeRateGainsStoreFactory.get(account = testAccountA, year = 2015),
      updatesWithImpact = ListMap(
        // Seed random fluctuating prices
        Add(createExchangeRateMeasurement(year = 2014)) -> StateImpact.NoChange,
        Add(createExchangeRateMeasurement(year = 2015, month = FEBRUARY)) -> StateImpact.NoChange,
        Add(createExchangeRateMeasurement(year = 2015, month = JULY)) -> StateImpact.NoChange,
        // Add Transactions and BalanceChecks
        Add(
          createTransaction(
            id = 10,
            year = 2015,
            beneficiary = testAccountB, // Shouldn't matter
            reservoir = testReservoirCashGbp
          )) -> StateImpact.Change,
        Add(createTransaction(id = 9, year = 2015, reservoir = testReservoirCashGbp)) -> StateImpact.Change,
        Add(createTransaction(id = 1, year = 2001, reservoir = testReservoirCashGbp)) -> StateImpact.Change,
        Add(createTransaction(id = 8, year = 2014, reservoir = testReservoirCashGbp)) -> StateImpact.Change,
        Add(createBalanceCheck(id = 7, year = 2013, reservoir = testReservoirCashGbp)) -> StateImpact.Change,
        Add(createTransaction(id = 6, year = 2012, reservoir = testReservoirCashGbp)) -> StateImpact.NoChange,
        Add(createTransaction(id = 5, year = 2012, reservoir = testReservoirCashGbp)) -> StateImpact.NoChange,
        Add(createBalanceCheck(id = 4, year = 2011, reservoir = testReservoirCashGbp)) -> StateImpact.NoChange,
        // Adding irrelevant Transactions and BalanceChecks
        Add(createTransaction(id = 11, year = 2015)) -> StateImpact.NoChange,
        Add(createBalanceCheck(id = 12, year = 2014)) -> StateImpact.NoChange,
        // Remove Transactions and BalanceChecks
        Remove[Transaction](1) -> StateImpact.NoChange,
        Remove[Transaction](6) -> StateImpact.NoChange,
        Remove[BalanceCheck](7) -> StateImpact.Change,
        Remove[Transaction](5) -> StateImpact.Change,
        Remove[BalanceCheck](4) -> StateImpact.Change,
        // Removing irrelevant Transactions and BalanceChecks
        Remove[Transaction](11) -> StateImpact.NoChange,
        Remove[BalanceCheck](12) -> StateImpact.NoChange
      )
    )

    "SummaryForYearStoreFactory" - runTest(
      store = testModule.summaryForYearStoreFactory.get(account = testAccountA, year = 2015),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, year = 2015, beneficiary = testAccountA)) -> StateImpact.Change,
        Add(createTransaction(id = 9, year = 2015, beneficiary = testAccountA)) -> StateImpact.Change,
        // Add irrelevant Transactions
        Add(createTransaction(id = 8, year = 2014, beneficiary = testAccountA)) -> StateImpact.NoChange,
        Add(createTransaction(id = 7, year = 2015, beneficiary = testAccountB)) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](10) -> StateImpact.Change,
        // Remove irrelevant Transactions
        Remove[Transaction](7) -> StateImpact.NoChange,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "SummaryYearsStoreFactory" - runTest(
      store = testModule.summaryYearsStoreFactory.get(testAccountA),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, year = 2015, month = DECEMBER, beneficiary = testAccountA)) -> StateImpact.Change,
        Add(createTransaction(id = 9, year = 2015, month = MARCH, beneficiary = testAccountA)) -> StateImpact.NoChange,
        Add(createTransaction(id = 8, year = 2015, month = JANUARY, beneficiary = testAccountA)) -> StateImpact.NoChange,
        Add(createTransaction(id = 7, year = 2013, beneficiary = testAccountA)) -> StateImpact.Change,
        Add(createTransaction(id = 6, year = 2014, beneficiary = testAccountB)) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](6) -> StateImpact.NoChange,
        Remove[Transaction](10) -> StateImpact.NoChange,
        Remove[Transaction](8) -> StateImpact.NoChange,
        Remove[Transaction](9) -> StateImpact.Change,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )

    "TagsStoreFactory" - runTest(
      store = testModule.tagsStoreFactory.get(),
      updatesWithImpact = ListMap(
        // Add Transactions
        Add(createTransaction(id = 10, tags = Seq())) -> StateImpact.NoChange,
        Add(createTransaction(id = 9, tags = Seq("a"))) -> StateImpact.Change,
        Add(createTransaction(id = 8, tags = Seq("a"))) -> StateImpact.Change,
        Add(createTransaction(id = 7, tags = Seq("b"))) -> StateImpact.Change,
        Add(createTransaction(id = 6, tags = Seq())) -> StateImpact.NoChange,
        // Remove Transactions
        Remove[Transaction](6) -> StateImpact.NoChange,
        Remove[Transaction](9) -> StateImpact.Change,
        Remove[Transaction](8) -> StateImpact.Change,
        Remove[Transaction](10) -> StateImpact.NoChange,
        // Add BalanceChecks
        Add(createBalanceCheck(id = 11)) -> StateImpact.NoChange,
        // Remove BalanceChecks
        Remove[BalanceCheck](11) -> StateImpact.NoChange
      )
    )
  }

  private def runTest(store: EntriesStore[_], updatesWithImpact: ListMap[EntityModification, StateImpact])(
      implicit entityAccess: FakeJsEntityAccess): Future[Unit] = async {
    def checkRemovingExistingEntity(update: EntityModification): Unit = {

      def checkIfIdExists[E <: Entity: EntityType](id: Long): Unit = {
        val existing = entityAccess.newQuerySync[E]().findOne(ModelFields.id[E] === id)
        require(existing.isDefined, s"Could not find entity of ${update.entityType} with id $id")
      }

      update match {
        case Remove(id) if update.entityType == Transaction.Type =>
          checkIfIdExists[Transaction](id)
        case Remove(id) if update.entityType == BalanceCheck.Type =>
          checkIfIdExists[BalanceCheck](id)
        case _ => // Do nothing
      }
    }

    var lastState = await(store.stateFuture)

    Future.sequence(
      for ((update, stateImpact) <- updatesWithImpact)
        yield
          async {
            checkRemovingExistingEntity(update)

            entityAccess.persistModifications(update)

            val newState = await(store.stateFuture)

            stateImpact match {
              case StateImpact.NoChange =>
                Predef.assert(
                  removeImpactingIds(newState) == removeImpactingIds(lastState),
                  s"For update $update:\n" +
                    s"Expected states to be the same (ignoring impacting IDs).\n" +
                    s"Previous: $lastState\n" +
                    s"Current:  ${newState}\n"
                )
              case StateImpact.Change =>
                Predef.assert(
                  removeImpactingIds(newState) != removeImpactingIds(lastState),
                  s"For update $update:\n" +
                    s"Expected states to be different (ignoring impacting IDs).\n" +
                    s"Previous: $lastState\n" +
                    s"Current:  ${newState}\n"
                )
              case StateImpact.Undefined =>
            }

            lastState = newState
          })
  }

  private def removeImpactingIds(state: Any): EntriesStore.StateTrait = {
    state match {
      case s: EntriesListStoreFactory.State[_] =>
        s.copy(impactingTransactionIds = Set(), impactingBalanceCheckIds = Set())
      case s: GainsForYear =>
        s.copy(impactingTransactionIds = Set(), impactingBalanceCheckIds = Set())
      case s: SummaryForYear =>
        s
      case s: SummaryYearsStoreFactory.State =>
        s.copy(impactingTransactionIds = Set())
      case s: TagsStoreFactory.State =>
        s
    }
  }

  private sealed trait StateImpact
  private object StateImpact {
    object NoChange extends StateImpact
    object Change extends StateImpact
    object Undefined extends StateImpact
  }

  private final class ThisTestModule extends TestModule {

    implicit private val complexQueryFilter = new ComplexQueryFilter

    val allEntriesStoreFactory = new AllEntriesStoreFactory
    val cashFlowEntriesStoreFactory = new CashFlowEntriesStoreFactory
    val complexQueryStoreFactory = new ComplexQueryStoreFactory
    val endowmentEntriesStoreFactory = new EndowmentEntriesStoreFactory
    val liquidationEntriesStoreFactory = new LiquidationEntriesStoreFactory
    val summaryExchangeRateGainsStoreFactory = new SummaryExchangeRateGainsStoreFactory
    val summaryForYearStoreFactory = new SummaryForYearStoreFactory
    val summaryYearsStoreFactory = new SummaryYearsStoreFactory
    val tagsStoreFactory = new TagsStoreFactory
  }
}

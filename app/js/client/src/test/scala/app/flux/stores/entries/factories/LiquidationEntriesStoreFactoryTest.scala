package app.flux.stores.entries.factories

import java.time.Month.JANUARY

import app.common.money.ReferenceMoney
import app.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.flux.stores.entries.AccountPair
import app.flux.stores.entries.LiquidationEntry
import app.models.accounting._
import app.models.accounting.config.Account
import app.models.accounting.config.MoneyReservoir
import app.models.modification.EntityModification
import hydro.common.time.LocalDateTimes.createDateTime
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object LiquidationEntriesStoreFactoryTest extends TestSuite {

  val pair = AccountPair(testAccountA, testAccountB)

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val exchangeRateManager = testModule.exchangeRateManager
    val factory: LiquidationEntriesStoreFactory = new LiquidationEntriesStoreFactory()

    "empty result" - async {
      val store = factory.get(pair, maxNumEntries = 10000)
      val state = await(store.stateFuture)

      state.entries ==> Seq()
      state.hasMore ==> false
    }

    "gives correct results" - async {
      val trans1 = persistTransaction(
        groupId = 1,
        flowInCents = 201,
        day = 10,
        account = testAccountA,
        reservoir = testReservoirOfAccountB)
      val trans2 = persistTransaction(
        groupId = 2,
        flowInCents = 202,
        day = 11,
        account = testAccountA,
        reservoir = testReservoirOfAccountB)
      val trans3 = persistTransaction(
        groupId = 2,
        flowInCents = 203,
        day = 12,
        account = testAccountB,
        reservoir = testReservoirOfAccountA)
      val trans4 = persistTransaction(
        groupId = 3,
        flowInCents = 4,
        day = 13,
        account = testAccountA,
        reservoir = testReservoirOfAccountB)
      val trans5 = persistTransaction(
        groupId = 4,
        flowInCents = 5,
        day = 14,
        account = testAccountB,
        reservoir = testReservoirOfAccountA)
      val trans6 = persistTransaction(
        groupId = 5,
        flowInCents = 6,
        day = 15,
        account = testAccountA,
        reservoir = testReservoirOfAccountB)

      // Get expectations
      val expectedEntries = Vector(
        LiquidationEntry(Seq(trans1), ReferenceMoney(-201)),
        LiquidationEntry(Seq(trans2, trans3), ReferenceMoney(-200)),
        LiquidationEntry(Seq(trans4), ReferenceMoney(-204)),
        LiquidationEntry(Seq(trans5), ReferenceMoney(-199)),
        LiquidationEntry(Seq(trans6), ReferenceMoney(-205))
      )

      // Run tests
      // Increasing number of entries
      await(
        Future.sequence(
          for (i <- 1 to expectedEntries.size)
            yield
              async {
                val subList = expectedEntries.takeRight(i)

                val state = await(factory.get(pair, maxNumEntries = subList.size).stateFuture)
                state.entries.map(_.entry) ==> subList
                state.hasMore ==> (i < expectedEntries.size)
              }))

      // All entries
      val allEntriesState = await(factory.get(pair, maxNumEntries = 10000).stateFuture)
      allEntriesState.entries.map(_.entry) ==> expectedEntries
      allEntriesState.hasMore ==> false
    }
  }

  private def persistTransaction(
      groupId: Long,
      flowInCents: Long,
      day: Int,
      account: Account = testAccountA,
      reservoir: MoneyReservoir)(implicit entityAccess: FakeJsEntityAccess): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(EntityModification.generateRandomId()),
      transactionGroupId = groupId,
      flowInCents = flowInCents,
      createdDate = createDateTime(2012, JANUARY, day),
      transactionDate = createDateTime(2012, JANUARY, day),
      consumedDate = createDateTime(2012, JANUARY, day),
      beneficiaryAccountCode = account.code,
      moneyReservoirCode = reservoir.code
    )
    entityAccess.addRemotelyAddedEntities(transaction)
    transaction
  }
}

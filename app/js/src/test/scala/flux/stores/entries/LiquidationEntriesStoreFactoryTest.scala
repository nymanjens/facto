package flux.stores.entries

import java.time.Month.JANUARY

import common.money.ReferenceMoney
import common.testing.TestObjects._
import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.accounting.config.{Account, MoneyReservoir}
import models.manager.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object LiquidationEntriesStoreFactoryTest extends TestSuite {

  val pair = AccountPair(testAccountA, testAccountB)

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    implicit val exchangeRateManager = testModule.exchangeRateManager
    implicit val entityAccess = testModule.entityAccess
    val factory: LiquidationEntriesStoreFactory = new LiquidationEntriesStoreFactory()

    "empty result" - {
      factory.get(pair, maxNumEntries = 10000).state.entries ==> Seq()
      factory.get(pair, maxNumEntries = 10000).state.hasMore ==> false
    }

    "gives correct results" - {
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
      "Increasing number of entries" - {
        for (i <- 1 to expectedEntries.size) {
          val subList = expectedEntries.takeRight(i)

          factory.get(pair, maxNumEntries = subList.size).state.entries ==> subList
          factory.get(pair, maxNumEntries = subList.size).state.hasMore ==> (i < expectedEntries.size)
        }
      }

      "All entries" - {
        factory.get(pair, maxNumEntries = 10000).state.entries ==> expectedEntries
        factory.get(pair, maxNumEntries = 10000).state.hasMore ==> false
      }
    }
  }

  private def persistTransaction(
      groupId: Long,
      flowInCents: Long,
      day: Int,
      account: Account = testAccountA,
      reservoir: MoneyReservoir)(implicit database: FakeRemoteDatabaseProxy): Transaction = {
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
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
}

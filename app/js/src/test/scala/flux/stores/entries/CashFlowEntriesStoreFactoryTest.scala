package flux.stores.entries

import java.time.Duration
import java.time.Month.JANUARY

import common.money.{Currency, MoneyWithGeneralCurrency}
import common.testing.TestObjects._
import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import common.time.LocalDateTimes.createDateTime
import flux.stores.entries.CashFlowEntry.{BalanceCorrection, RegularEntry}
import models.accounting._
import models.accounting.config.MoneyReservoir
import models.modification.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object CashFlowEntriesStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    implicit val exchangeRateManager = testModule.exchangeRateManager
    implicit val entityAccess = testModule.entityAccess
    val factory: CashFlowEntriesStoreFactory = new CashFlowEntriesStoreFactory()

    "empty result" - {
      factory.get(testReservoir, maxNumEntries = 10000).state.entries ==> Seq()
      factory.get(testReservoir, maxNumEntries = 10000).state.hasMore ==> false
    }

    "gives correct results" - {
      // get and persist dummy transactions/BCs
      val trans1 = persistTransaction(groupId = 1, flowInCents = 200, day = 1)
      val bc1 = persistBalanceCheck(balanceInCents = 20, day = 2)
      val trans2 = persistTransaction(groupId = 2, flowInCents = 300, day = 3)
      val trans3 = persistTransaction(groupId = 2, flowInCents = 100, day = 4)
      val bc2 = persistBalanceCheck(balanceInCents = 20, day = 5)
      val bc3 = persistBalanceCheck(balanceInCents = 30, day = 6)
      persistBalanceCheck(balanceInCents = 30, day = 7)
      persistBalanceCheck(balanceInCents = 30, day = 8)
      val trans4 = persistTransaction(groupId = 1, flowInCents = -200, day = 9)
      persistBalanceCheck(balanceInCents = -170, day = 10)
      val trans5 = persistTransaction(groupId = 5, flowInCents = -50, day = 11)
      val trans6 = persistTransaction(groupId = 6, flowInCents = -30, day = 12)
      persistBalanceCheck(balanceInCents = -250, day = 13)

      persistBalanceCheck(balanceInCents = 20, day = 30, reservoir = otherTestReservoir)
      persistTransaction(groupId = 10, flowInCents = 29989, day = 30, reservoir = otherTestReservoir)

      // get expectations
      val expectedEntries = Vector(
        RegularEntry(Seq(trans1), MoneyWithGeneralCurrency(200, Currency.default), balanceVerified = false),
        BalanceCorrection(bc1, MoneyWithGeneralCurrency(200, Currency.default)),
        RegularEntry(
          Seq(trans2, trans3),
          MoneyWithGeneralCurrency(420, Currency.default),
          balanceVerified = false),
        BalanceCorrection(bc2, MoneyWithGeneralCurrency(420, Currency.default)),
        BalanceCorrection(bc3, MoneyWithGeneralCurrency(20, Currency.default)),
        RegularEntry(Seq(trans4), MoneyWithGeneralCurrency(-170, Currency.default), balanceVerified = true),
        RegularEntry(Seq(trans5), MoneyWithGeneralCurrency(-220, Currency.default), balanceVerified = false),
        RegularEntry(Seq(trans6), MoneyWithGeneralCurrency(-250, Currency.default), balanceVerified = true)
      )

      // Run tests
      "Increasing number of entries" - {
        for (i <- 1 to expectedEntries.size) {
          val subList = expectedEntries.takeRight(i)

          factory.get(testReservoir, maxNumEntries = subList.size).state.entries ==> subList
          factory
            .get(testReservoir, maxNumEntries = subList.size)
            .state
            .hasMore ==> (i < expectedEntries.size)
        }
      }

      "All entries" - {
        factory.get(testReservoir, maxNumEntries = 10000).state.entries ==> expectedEntries
        factory.get(testReservoir, maxNumEntries = 10000).state.hasMore ==> false
      }
    }

    "Overlapping days" - {
      val trans1 = persistTransaction(groupId = 1, day = 1, flowInCents = 200, createIncrement = 1)
      val bc1 = persistBalanceCheck(balanceInCents = 20, day = 1, createIncrement = 2)
      val trans2 = persistTransaction(groupId = 2, flowInCents = 300, day = 1, createIncrement = 3)
      val trans3 = persistTransaction(groupId = 2, flowInCents = 100, day = 1, createIncrement = 4)
      val bc2 = persistBalanceCheck(balanceInCents = 20, day = 1, createIncrement = 5)
      val bc3 = persistBalanceCheck(balanceInCents = 30, day = 1, createIncrement = 6)
      persistBalanceCheck(balanceInCents = 30, day = 1, createIncrement = 7)
      persistBalanceCheck(balanceInCents = 30, day = 1, createIncrement = 8)
      val trans4 = persistTransaction(groupId = 1, flowInCents = -200, day = 1, createIncrement = 9)
      persistBalanceCheck(balanceInCents = -170, day = 1, createIncrement = 10)
      val trans5 = persistTransaction(groupId = 5, flowInCents = -50, day = 1, createIncrement = 11)
      val trans6 = persistTransaction(groupId = 6, flowInCents = -30, day = 1, createIncrement = 12)
      persistBalanceCheck(balanceInCents = -250, day = 1, createIncrement = 13)

      val expectedEntries = Vector(
        RegularEntry(Seq(trans1), MoneyWithGeneralCurrency(200, Currency.default), balanceVerified = false),
        BalanceCorrection(bc1, MoneyWithGeneralCurrency(200, Currency.default)),
        RegularEntry(
          Seq(trans2, trans3),
          MoneyWithGeneralCurrency(420, Currency.default),
          balanceVerified = false),
        BalanceCorrection(bc2, MoneyWithGeneralCurrency(420, Currency.default)),
        BalanceCorrection(bc3, MoneyWithGeneralCurrency(20, Currency.default)),
        RegularEntry(Seq(trans4), MoneyWithGeneralCurrency(-170, Currency.default), balanceVerified = true),
        RegularEntry(Seq(trans5), MoneyWithGeneralCurrency(-220, Currency.default), balanceVerified = false),
        RegularEntry(Seq(trans6), MoneyWithGeneralCurrency(-250, Currency.default), balanceVerified = true)
      )

      factory.get(testReservoir, maxNumEntries = 10000).state.entries ==> expectedEntries
    }
  }

  private def persistTransaction(
      groupId: Long,
      flowInCents: Long,
      day: Int,
      reservoir: MoneyReservoir = testReservoir,
      createIncrement: Int = 0)(implicit database: FakeRemoteDatabaseProxy): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(EntityModification.generateRandomId()),
      transactionGroupId = groupId,
      flowInCents = flowInCents,
      createdDate = createDateTime(2012, JANUARY, day).plus(Duration.ofSeconds(createIncrement)),
      transactionDate = createDateTime(2012, JANUARY, day),
      consumedDate = createDateTime(2012, JANUARY, day),
      moneyReservoirCode = reservoir.code
    )
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
  private def persistBalanceCheck(
      balanceInCents: Long,
      day: Int,
      reservoir: MoneyReservoir = testReservoir,
      createIncrement: Int = 0)(implicit database: FakeRemoteDatabaseProxy): BalanceCheck = {
    val balanceCheck = testBalanceCheckWithId.copy(
      idOption = Some(EntityModification.generateRandomId()),
      balanceInCents = balanceInCents,
      createdDate = createDateTime(2012, JANUARY, day).plus(Duration.ofSeconds(createIncrement)),
      checkDate = createDateTime(2012, JANUARY, day),
      moneyReservoirCode = reservoir.code
    )
    database.addRemotelyAddedEntities(balanceCheck)
    balanceCheck
  }
}

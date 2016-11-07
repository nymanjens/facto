package controllers.helpers.accounting

import com.google.inject._

import scala.collection.immutable.Seq
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing.{FactoTestModule, HookedSpecification}
import models.accounting.money.ReferenceMoney
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class LiquidationEntryTest extends HookedSpecification {

  @Inject val liquidationEntries: LiquidationEntries = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "fetchLastNEntries()" in new WithApplication {
    // Get and persist dummy transactions
    // Irrelevant transactions (should be ignored)
    persistTransaction(groupId = 91, account = testAccountA, reservoir = testReservoirOfAccountA)
    persistTransaction(groupId = 92, account = testAccountB, reservoir = testReservoirOfAccountB)

    // Relevant
    val trans1 = persistTransaction(groupId = 1, flowInCents = 201, timestamp = 1000, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans2 = persistTransaction(groupId = 2, flowInCents = 202, timestamp = 1010, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans3 = persistTransaction(groupId = 2, flowInCents = 203, timestamp = 1020, account = testAccountB, reservoir = testReservoirOfAccountA)
    val trans4 = persistTransaction(groupId = 3, flowInCents = 4, timestamp = 1030, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans5 = persistTransaction(groupId = 4, flowInCents = 5, timestamp = 1040, account = testAccountB, reservoir = testReservoirOfAccountA)
    val trans6 = persistTransaction(groupId = 5, flowInCents = 6, timestamp = 1050, account = testAccountA, reservoir = testReservoirOfAccountB)

    // Get expectations
    val expectedEntries = Vector(
      LiquidationEntry(Seq(trans1), ReferenceMoney(-201)),
      LiquidationEntry(Seq(trans2, trans3), ReferenceMoney(-200)),
      LiquidationEntry(Seq(trans4), ReferenceMoney(-204)),
      LiquidationEntry(Seq(trans5), ReferenceMoney(-199)),
      LiquidationEntry(Seq(trans6), ReferenceMoney(-205))
    )

    // Run tests
    val pair = AccountPair(testAccountA, testAccountB)
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      liquidationEntries.fetchLastNEntries(pair, n = subList.size) mustEqual subList
    }

    // test when n > num entries
    liquidationEntries.fetchLastNEntries(pair, n = 1000) mustEqual expectedEntries
  }
}

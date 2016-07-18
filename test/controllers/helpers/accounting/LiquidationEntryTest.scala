package controllers.helpers.accounting

import scala.collection.immutable.Seq
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import models.accounting._
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class LiquidationEntryTest extends Specification {
  "fetchLastNEntries()" in new WithApplication {
    // Get and persist dummy transactions
    // Irrelevant transactions (should be ignored)
    persistTransaction(groupId = 91, account = testAccountA, reservoir = testReservoirOfAccountA)
    persistTransaction(groupId = 92, account = testAccountB, reservoir = testReservoirOfAccountB)

    // Relevant
    val trans1 = persistTransaction(groupId = 1, Money(201), timestamp = 1000, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans2 = persistTransaction(groupId = 2, Money(202), timestamp = 1010, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans3 = persistTransaction(groupId = 2, Money(203), timestamp = 1020, account = testAccountB, reservoir = testReservoirOfAccountA)
    val trans4 = persistTransaction(groupId = 3, Money(4), timestamp = 1030, account = testAccountA, reservoir = testReservoirOfAccountB)
    val trans5 = persistTransaction(groupId = 4, Money(5), timestamp = 1040, account = testAccountB, reservoir = testReservoirOfAccountA)
    val trans6 = persistTransaction(groupId = 5, Money(6), timestamp = 1050, account = testAccountA, reservoir = testReservoirOfAccountB)

    // Get expectations
    val expectedEntries = Vector(
      LiquidationEntry(Seq(trans1), Money(-201)),
      LiquidationEntry(Seq(trans2, trans3), Money(-200)),
      LiquidationEntry(Seq(trans4), Money(-204)),
      LiquidationEntry(Seq(trans5), Money(-199)),
      LiquidationEntry(Seq(trans6), Money(-205))
    )

    // Run tests
    val pair = AccountPair(testAccountA, testAccountB)
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      LiquidationEntry.fetchLastNEntries(pair, n = subList.size) mustEqual subList
    }

    // test when n > num entries
    LiquidationEntry.fetchLastNEntries(pair, n = 1000) mustEqual expectedEntries
  }
}

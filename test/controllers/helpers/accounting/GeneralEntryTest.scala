package controllers.helpers.accounting

import scala.collection.immutable.Seq

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import org.joda.time.DateTime

import models.accounting._
import models.accounting.config.{Category, MoneyReservoir, Account}
import models.accounting.config.Config.constants.endowmentCategory
import common.testing.TestObjects._
import common.testing.TestUtils._

@RunWith(classOf[JUnitRunner])
class GeneralEntryTest extends Specification {

  "fetchLastNEntries()" in new WithApplication(fakeApplication) {
    // Get and persist dummy transactions
    val trans1 = persistTransaction(groupId = 1, flow = Money(200), timestamp = 1000)
    val trans2 = persistTransaction(groupId = 2, flow = Money(300), timestamp = 1020)
    val trans3 = persistTransaction(groupId = 2, flow = Money(100), timestamp = 1030)
    val trans4 = persistTransaction(groupId = 1, flow = Money(-200), timestamp = 1080)
    val trans5 = persistTransaction(groupId = 5, flow = Money(-50), timestamp = 1090)
    val trans6 = persistTransaction(groupId = 6, flow = Money(-30), timestamp = 1100)

    // Get expectations
    val expectedEntries = Vector(
      GeneralEntry(Seq(trans1)),
      GeneralEntry(Seq(trans2, trans3)),
      GeneralEntry(Seq(trans4)),
      GeneralEntry(Seq(trans5)),
      GeneralEntry(Seq(trans6))
    )

    // Run tests
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      GeneralEntry.fetchLastNEntries(n = subList.size) mustEqual subList
    }

    // Test when n > num entries
    GeneralEntry.fetchLastNEntries(n = 1000) mustEqual expectedEntries
  }

  "fetchLastNEndowments()" in new WithApplication(fakeApplication) {
    // Get and persist dummy transactions
    // Irrelevant transactions (should be ignored)
    persistTransaction(groupId = 91, account = testAccountB)
    persistTransaction(groupId = 92, account = testAccountA, category = testCategory)

    // Relevant
    val trans1 = persistTransaction(groupId = 1, Money(201), timestamp = 1000, account = testAccountA, category = endowmentCategory)
    val trans2 = persistTransaction(groupId = 2, Money(202), timestamp = 1010, account = testAccountA, category = endowmentCategory)
    val trans3 = persistTransaction(groupId = 2, Money(203), timestamp = 1020, account = testAccountA, category = endowmentCategory)
    val trans4 = persistTransaction(groupId = 3, Money(204), timestamp = 1030, account = testAccountA, category = endowmentCategory)
    val trans5 = persistTransaction(groupId = 4, Money(205), timestamp = 1040, account = testAccountA, category = endowmentCategory)
    val trans6 = persistTransaction(groupId = 5, Money(206), timestamp = 1050, account = testAccountA, category = endowmentCategory)

    // Get expectations
    val expectedEntries = Vector(
      GeneralEntry(Seq(trans1)),
      GeneralEntry(Seq(trans2, trans3)),
      GeneralEntry(Seq(trans4)),
      GeneralEntry(Seq(trans5)),
      GeneralEntry(Seq(trans6))
    )

    // Run tests
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      GeneralEntry.fetchLastNEndowments(testAccountA, n = subList.size) mustEqual subList
    }

    // test when n > num entries
    GeneralEntry.fetchLastNEndowments(testAccountA, n = 1000) mustEqual expectedEntries
  }
}

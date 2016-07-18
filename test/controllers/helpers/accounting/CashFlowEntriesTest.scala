package controllers.helpers.accounting

import collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Stack
import scala.util.Random
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joda.time.DateTime
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.accounting.config.MoneyReservoir
import models.accounting.config.Account
import models._
import models.accounting._
import models.SlickUtils.dbApi._
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class CashFlowEntriesTest extends Specification {

  "CashFlowEntry.fetchLastNEntries()" in new WithApplication {
    // get and persist dummy transactions/BCs
    val trans1 = persistTransaction(groupId = 1, flow = Money(200), timestamp = 1000)
    val bc1 = persistBalanceCheck(balance = Money(20), timestamp = 1010)
    val trans2 = persistTransaction(groupId = 2, flow = Money(300), timestamp = 1020)
    val trans3 = persistTransaction(groupId = 2, flow = Money(100), timestamp = 1030)
    val bc2 = persistBalanceCheck(balance = Money(20), timestamp = 1040)
    val bc3 = persistBalanceCheck(balance = Money(30), timestamp = 1050)
    val bc4 = persistBalanceCheck(balance = Money(30), timestamp = 1060)
    val bc5 = persistBalanceCheck(balance = Money(30), timestamp = 1070)
    val trans4 = persistTransaction(groupId = 1, flow = Money(-200), timestamp = 1080)
    val bc6 = persistBalanceCheck(balance = Money(-170), timestamp = 1085)
    val trans5 = persistTransaction(groupId = 5, flow = Money(-50), timestamp = 1090)
    val trans6 = persistTransaction(groupId = 6, flow = Money(-30), timestamp = 1100)
    val bc7 = persistBalanceCheck(balance = Money(-250), timestamp = 1110)

    persistBalanceCheck(balance = Money(20), timestamp = 99000, reservoir = otherTestReservoir)
    persistTransaction(groupId = 10, flow = Money(29989), timestamp = 99000, reservoir = otherTestReservoir)

    // get expectations
    val expectedEntries = Vector(
      RegularEntry(Seq(trans1), Money(200), false),
      BalanceCorrection(bc1),
      RegularEntry(Seq(trans2, trans3), Money(420), false),
      BalanceCorrection(bc2),
      BalanceCorrection(bc3),
      RegularEntry(Seq(trans4), Money(-170), true),
      RegularEntry(Seq(trans5), Money(-220), false),
      RegularEntry(Seq(trans6), Money(-250), true)
    )

    // run tests
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      CashFlowEntry.fetchLastNEntries(testReservoir, n = subList.size) mustEqual subList
    }

    // test when n > num entries
    CashFlowEntry.fetchLastNEntries(testReservoir, n = 1000) mustEqual expectedEntries
  }

  "CashFlowEntry.fetchLastNEntries() with large database" in new WithApplication {
    // get and persist dummy transactions/BCs
    for (i <- 1 to 20 * 1000) {
      if (i % 1000 == 0) {
        Logger.info(s"Persisting entries... (persisted $i)")
      }
      persistTransaction(groupId = i, flow = Money(Random.nextInt), timestamp = i)
      persistBalanceCheck(balance = Money(Random.nextInt), timestamp = i)
    }

    CashFlowEntry.fetchLastNEntries(testReservoir, n = 4000) must haveSize(4000)
    CashFlowEntry.fetchLastNEntries(testReservoir, n = 15 * 1000) must haveSize(15 * 1000)
    CashFlowEntry.fetchLastNEntries(testReservoir, n = 35 * 1000) must haveSize(35 * 1000)
  }
}

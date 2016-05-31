package models.accounting

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import org.joda.time.DateTime

import common.Clock
import common.TimeUtils.{dateAt, April}
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing.HookedSpecification
import models._

@RunWith(classOf[JUnitRunner])
class UpdateLogTest extends HookedSpecification {

  override def afterAll = Clock.cleanupAfterTest

  "UpdateLogs.fetchLastNEntries" in new WithApplication {
    // add logs
    Clock.setTimeForTest(dateAt(2016, April, 1))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(111))
    Clock.setTimeForTest(dateAt(2016, April, 2))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(222))
    Clock.setTimeForTest(dateAt(2016, April, 3))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(333))
    Clock.setTimeForTest(dateAt(2016, April, 4))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(444))
    Clock.setTimeForTest(dateAt(2016, April, 5))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(555))
    Clock.setTimeForTest(dateAt(2016, April, 6))
    UpdateLogs.addLog(testUser, UpdateLogs.Edit, balanceCheck(666))
    Clock.setTimeForTest(dateAt(2016, April, 7))
    UpdateLogs.addLog(testUser, UpdateLogs.Delete, balanceCheck(777))

    // fetch logs
    val entries = UpdateLogs.fetchLastNEntries(n = 3)

    // check result
    entries must haveSize(3)
    for (entry <- entries) entry.user mustEqual testUser
    entries(0).date mustEqual dateAt(2016, April, 5)
    entries(1).date mustEqual dateAt(2016, April, 6)
    entries(2).date mustEqual dateAt(2016, April, 7)
  }

  "Logged TransactionGroup contains all relevant info" in new WithApplication {
    // add logs
    Clock.setTimeForTest(dateAt(2016, April, 1))
    val transGrp = TransactionGroups.all.add(TransactionGroup())
    Transactions.all.add(Transaction(
      transactionGroupId = transGrp.id,
      issuerId = testUser.id,
      beneficiaryAccountCode = testAccount.code,
      moneyReservoirCode = testReservoir.code,
      categoryCode = testCategory.code,
      description = "test description",
      flow = Money(9199),
      transactionDate = dateAt(2014, April, 1),
      consumedDate = dateAt(2015, April, 1)
    ))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, transGrp)

    // fetch logs
    val entries = UpdateLogs.fetchLastNEntries(n = 3)

    // check result
    entries must haveSize(1)
    val change = entries(0).change
    change must contain(transGrp.id.toString)
    change must contain(testUser.loginName)
    change must contain(testAccount.code)
    change must contain(testReservoir.code)
    change must contain(testCategory.code)
    change must contain("test description")
    change must contain("91.99")
    change must contain("2014")
    change must contain("2015")
    change must contain("2016")
  }

  "Logged BalanceCheck contains all relevant info" in new WithApplication {
    // add logs
    Clock.setTimeForTest(dateAt(2016, April, 1))
    UpdateLogs.addLog(testUser, UpdateLogs.AddNew, balanceCheck(8788))

    // fetch logs
    val entries = UpdateLogs.fetchLastNEntries(n = 3)

    // check result
    entries must haveSize(1)
    val change = entries(0).change
    change must contain(testUser.loginName)
    change must contain(testReservoir.code)
    change must contain("87.88")
    change must contain("2016")
  }

  private def balanceCheck(balance: Long): BalanceCheck = {
    BalanceChecks.all.add(BalanceCheck(
      issuerId = testUser.id,
      moneyReservoirCode = testReservoir.code,
      balance = Money(balance),
      checkDate = Clock.now))
  }
}

// TODO: Fix this test

//package models.accounting
//
//import com.google.inject._
//import org.specs2.mutable._
//import org.specs2.runner._
//import org.junit.runner._
//import play.api.test._
//import java.time.Month._
//import common.time.Clock
//import common.time.LocalDateTimes.createDateTime
//import common.TimeUtils.{April, dateAt}
//import common.testing.TestObjects._
//import common.testing.TestUtils._
//import common.testing._
//import common.testing.HookedSpecification
//import models._
//import models.accounting.money.Money
//
//@RunWith(classOf[JUnitRunner])
//class SlickUpdateLogManagerTest extends HookedSpecification {
//
//  @Inject implicit val entityAccess: EntityAccess = null
//  @Inject val updateLogManager: SlickUpdateLogManager = null
//  @Inject val transactionManager: Transaction.Manager = null
//  @Inject val transactionGroupManager: TransactionGroup.Manager = null
//  @Inject val balanceCheckManager: BalanceCheck.Manager = null
//
//  override def before() = {
//    Guice.createInjector(new FactoTestModule).injectMembers(this)
//  }
//
//  override def afterAll = Clock.cleanupAfterTest()
//
//  "SlickUpdateLogManager.fetchLastNEntries" in new WithApplication {
//    // add logs
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 1))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(111))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 2))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(222))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 3))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(333))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 4))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(444))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 5))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(555))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 6))
//    updateLogManager.addLog(testUser, UpdateLog.Edit, balanceCheck(666))
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 7))
//    updateLogManager.addLog(testUser, UpdateLog.Delete, balanceCheck(777))
//
//    // fetch logs
//    val entries = updateLogManager.fetchLastNEntries(n = 3)
//
//    // check result
//    entries must haveSize(3)
//    for (entry <- entries) entry.user mustEqual testUser
//    entries(0).date mustEqual createDateTime(2016, APRIL, 5)
//    entries(1).date mustEqual createDateTime(2016, APRIL, 6)
//    entries(2).date mustEqual createDateTime(2016, APRIL, 7)
//  }
//
//  "Logged TransactionGroup contains all relevant info" in new WithApplication {
//    // add logs
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 1))
//    val transGrp = transactionGroupManager.add(TransactionGroup(createdDate = clock.now))
//    transactionManager.add(Transaction(
//      transactionGroupId = transGrp.id,
//      issuerId = testUser.id,
//      beneficiaryAccountCode = testAccount.code,
//      moneyReservoirCode = testReservoir.code,
//      categoryCode = testCategory.code,
//      description = "test description",
//      flowInCents = 9199,
//      transactionDate = createDateTime(2014, APRIL, 1),
//      consumedDate = createDateTime(2015, APRIL, 1)
//    ))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, transGrp)
//
//    // fetch logs
//    val entries = updateLogManager.fetchLastNEntries(n = 3)
//
//    // check result
//    entries must haveSize(1)
//    val change = entries(0).change
//    change must contain(transGrp.id.toString)
//    change must contain(testUser.loginName)
//    change must contain(testAccount.code)
//    change must contain(testReservoir.code)
//    change must contain(testCategory.code)
//    change must contain("test description")
//    change must contain("91.99")
//    change must contain("2014")
//    change must contain("2015")
//    change must contain("2016")
//  }
//
//  "Logged BalanceCheck contains all relevant info" in new WithApplication {
//    // add logs
//    Clock.setTimeForTest(createDateTime(2016, APRIL, 1))
//    updateLogManager.addLog(testUser, UpdateLog.AddNew, balanceCheck(8788))
//
//    // fetch logs
//    val entries = updateLogManager.fetchLastNEntries(n = 3)
//
//    // check result
//    entries must haveSize(1)
//    val change = entries(0).change
//    change must contain(testUser.loginName)
//    change must contain(testReservoir.code)
//    change must contain("87.88")
//    change must contain("2016")
//  }
//
//  private def balanceCheck(balance: Long): BalanceCheck = {
//    balanceCheckManager.add(BalanceCheck(
//      issuerId = testUser.id,
//      moneyReservoirCode = testReservoir.code,
//      balanceInCents = balance,
//      checkDate = clock.now))
//  }
//}

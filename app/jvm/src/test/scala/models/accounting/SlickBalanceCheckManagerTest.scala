package models.accounting

import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import common.time.Clock
import models._
import models.modificationhandler.EntityModificationHandler
import models.user.SlickUserManager
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class SlickBalanceCheckManagerTest extends HookedSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject implicit private val entityModificationHandler: EntityModificationHandler = null
  @Inject private val userManager: SlickUserManager = null

  @Inject private val balanceCheckManager: SlickBalanceCheckManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the BalanceCheck model" in new WithApplication {
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)

    // get and persist dummy balanceCheckManager
    val checkA1 = TestUtils.persist(
      BalanceCheck(
        issuerId = testUserA.id,
        moneyReservoirCode = "ACC_A",
        balanceInCents = 999,
        createdDate = clock.now,
        checkDate = localDateTimeOfEpochMilli(1000)
      ))
    val checkA2 = TestUtils.persist(
      BalanceCheck(
        issuerId = testUserA.id,
        moneyReservoirCode = "ACC_A",
        balanceInCents = 1000,
        createdDate = clock.now,
        checkDate = localDateTimeOfEpochMilli(2000)
      ))
    val checkB = TestUtils.persist(
      BalanceCheck(
        issuerId = testUserB.id,
        moneyReservoirCode = "ACC_B",
        balanceInCents = 999,
        createdDate = clock.now,
        checkDate = clock.now
      ))

    // do basic checks
    checkA1.issuer mustEqual testUserA
    checkA2.moneyReservoirCode mustEqual "ACC_A"
    balanceCheckManager.fetchAll() mustEqual Seq(checkA1, checkA2, checkB)
  }

  "test inserting a BC with ID" in new WithApplication {
    TestUtils.persist(testUser)

    val id = 12345
    val bc =
      BalanceCheck(
        issuerId = testUser.id,
        moneyReservoirCode = testReservoir.code,
        balanceInCents = 999,
        createdDate = clock.now,
        checkDate = localDateTimeOfEpochMilli(1000),
        idOption = Option(id)
      )
    balanceCheckManager.addIfNew(bc)

    bc.id mustEqual id
    balanceCheckManager.fetchAll() must haveSize(1)
    getOnlyElement(balanceCheckManager.fetchAll()).id mustEqual id
    balanceCheckManager.fetchAll() mustEqual Seq(bc)
  }
}

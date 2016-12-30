package models.accounting

import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import common.time.Clock
import models._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class SlickBalanceCheckManagerTest extends HookedSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject private val userManager: SlickUserManager = null

  @Inject private val balanceCheckManager: SlickBalanceCheckManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the BalanceCheck model" in new WithApplication {

    // prepare users
    val user1 = userManager.add(SlickUserManager.createUser(loginName = "tester", password = "x", name = "Tester"))
    val user2 = userManager.add(SlickUserManager.createUser(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy balanceCheckManager
    val checkA1 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 999,
      createdDate = clock.now,
      checkDate = localDateTimeOfEpochMilli(1000)
    ))
    val checkA2 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 1000,
      createdDate = clock.now,
      checkDate = localDateTimeOfEpochMilli(2000)
    ))
    val checkB = balanceCheckManager.add(BalanceCheck(
      issuerId = user2.id,
      moneyReservoirCode = "ACC_B",
      balanceInCents = 999,
      createdDate = clock.now,
      checkDate = clock.now
    ))

    // do basic checks
    checkA1.issuer mustEqual user1
    checkA2.moneyReservoirCode mustEqual "ACC_A"
    balanceCheckManager.fetchAll() mustEqual Seq(checkA1, checkA2, checkB)
  }

  "test inserting a BC with ID" in new WithApplication {
    val id = 12345
    val bc = balanceCheckManager.addWithId(BalanceCheck(
      issuerId = testUser.id,
      moneyReservoirCode = testReservoir.code,
      balanceInCents = 999,
      createdDate = clock.now,
      checkDate = localDateTimeOfEpochMilli(1000),
      idOption = Option(id)
    ))

    bc.id mustEqual id
    balanceCheckManager.fetchAll() must haveSize(1)
    getOnlyElement(balanceCheckManager.fetchAll()).id mustEqual id
    balanceCheckManager.fetchAll() mustEqual Seq(bc)

    balanceCheckManager.addWithId(BalanceCheck(
      issuerId = testUser.id,
      moneyReservoirCode = testReservoir.code,
      balanceInCents = 888888,
      createdDate = clock.now,
      checkDate = localDateTimeOfEpochMilli(1005),
      idOption = Option(id)
    )) must throwA[IllegalArgumentException]
  }
}

package models.accounting

import com.google.inject._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import java.time.Instant
import common.Clock
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models._
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class SlickBalanceCheckManagerTest extends HookedSpecification {

  @Inject implicit val entityAccess: EntityAccess = null
  @Inject val userManager: User.Manager = null

  @Inject val balanceCheckManager: SlickBalanceCheckManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the BalanceCheck model" in new WithApplication {

    // prepare users
    val user1 = userManager.add(userManager.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
    val user2 = userManager.add(userManager.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy balanceCheckManager
    val checkA1 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 999,
      checkDate = Instant.ofEpochMilli(1000)
    ))
    val checkA2 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 1000,
      checkDate = Instant.ofEpochMilli(2000)
    ))
    val checkB = balanceCheckManager.add(BalanceCheck(
      issuerId = user2.id,
      moneyReservoirCode = "ACC_B",
      balanceInCents = 999,
      checkDate = Clock.now
    ))

    // do basic checks
    checkA1.issuer mustEqual user1
    checkA2.moneyReservoirCode mustEqual "ACC_A"
    balanceCheckManager.fetchAll() mustEqual Seq(checkA1, checkA2, checkB)
  }
}

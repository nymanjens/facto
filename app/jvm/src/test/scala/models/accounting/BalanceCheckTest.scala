package models.accounting

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import org.joda.time.DateTime
import common.Clock
import common.testing.TestObjects._
import common.testing.TestUtils._
import models._
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class BalanceCheckTest extends Specification {

  "test the BalanceCheck model" in new WithApplication {

    // prepare users
    val user1 = Users.add(Users.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
    val user2 = Users.add(Users.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy balanceCheckManager
    val checkA1 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 999,
      checkDate = new DateTime(1000)
    ))
    val checkA2 = balanceCheckManager.add(BalanceCheck(
      issuerId = user1.id,
      moneyReservoirCode = "ACC_A",
      balanceInCents = 1000,
      checkDate = new DateTime(2000)
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

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

@RunWith(classOf[JUnitRunner])
class BalanceCheckTest extends Specification {

  "test the BalanceCheck model" in new WithApplication(fakeApplication) {

    // prepare users
    val user1 = Users.all.save(Users.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
    val user2 = Users.all.save(Users.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy BalanceChecks
    val checkA1 = BalanceChecks.all.save(BalanceCheck(
      issuerId = user1.id.get,
      moneyReservoirCode = "ACC_A",
      balance = Money(999),
      checkDate = new DateTime(1000)
    ))
    val checkA2 = BalanceChecks.all.save(BalanceCheck(
      issuerId = user1.id.get,
      moneyReservoirCode = "ACC_A",
      balance = Money(1000),
      checkDate = new DateTime(2000)
    ))
    val checkB = BalanceChecks.all.save(BalanceCheck(
      issuerId = user2.id.get,
      moneyReservoirCode = "ACC_B",
      balance = Money(999),
      checkDate = Clock.now
    ))

    // do basic checks
    checkA1.issuer mustEqual user1
    checkA2.moneyReservoirCode mustEqual "ACC_A"
    BalanceChecks.all.fetchAll mustEqual Seq(checkA1, checkA2, checkB)
  }
}

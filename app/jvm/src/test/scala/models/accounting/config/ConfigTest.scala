package models.accounting.config

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import common.testing.TestObjects._
import common.testing.TestUtils._
import models._

@RunWith(classOf[JUnitRunner])
class ConfigTest extends Specification {

  "test configuration parsing" in new WithApplication {
    // create test users
    val userA = Users.add(Users.newWithUnhashedPw(loginName = "a", password = "pw", name = "Test User A"))
    val userB = Users.add(Users.newWithUnhashedPw(loginName = "b", password = "pw", name = "Test User B"))
    val userOther = Users.add(Users.newWithUnhashedPw(loginName = "other", password = "other", name = "Other"))

    // check keys
    Config.accounts.keys.toList must beEqualTo(List("ACC_COMMON", "ACC_A", "ACC_B"))
    Config.categories.keys.toList must beEqualTo(List("CAT_B", "CAT_A"))
    Config.visibleReservoirs.map(_.code) must beEqualTo(List("CASH_COMMON", "CARD_COMMON", "CASH_A", "CARD_A", "CASH_B", "CARD_B"))

    // check content by samples
    Config.accounts("ACC_A").code must beEqualTo("ACC_A")
    Config.accounts("ACC_A").longName must beEqualTo("Account A")
    Config.accounts("ACC_A").categories must beEqualTo(List(Config.categories("CAT_A"), Config.categories("CAT_B")))
    Config.accounts("ACC_A").user mustEqual Some(userA)
    Config.accounts("ACC_COMMON").user mustEqual None

    // check common account
    Config.constants.commonAccount must beEqualTo(Config.accounts("ACC_COMMON"))

    // check accountOf()
    Config.accountOf(userA) must beEqualTo(Some(Config.accounts("ACC_A")))
    Config.accountOf(userB) must beEqualTo(Some(Config.accounts("ACC_B")))
    Config.accountOf(userOther) must beEqualTo(None)

    // test Account.isMineOrCommon()
    Config.accounts("ACC_A").isMineOrCommon(userA) mustEqual true
    Config.accounts("ACC_COMMON").isMineOrCommon(userA) mustEqual true
    Config.accounts("ACC_B").isMineOrCommon(userA) mustEqual false
  }


  "test Config.personallySortedAccounts()" in new WithApplication {

    // get vars
    val accCommon = Config.constants.commonAccount
    val accA = Config.accounts("ACC_A")
    val accB = Config.accounts("ACC_B")
    val userA = Users.add(Users.newWithUnhashedPw(loginName = "a", password = "a", name = "A"))
    // make sure all required users exist
    val userB = Users.add(Users.newWithUnhashedPw(loginName = "b", password = "b", name = "B"))
    val userOther = Users.add(Users.newWithUnhashedPw(loginName = "other", password = "other", name = "Other"))

    // call personallySortedAccounts()
    Config.personallySortedAccounts(userA) mustEqual Seq(accCommon, accA, accB)
    Config.personallySortedAccounts(userB) mustEqual Seq(accCommon, accB, accA)
    Config.personallySortedAccounts(userOther) mustEqual Seq(accCommon, accA, accB)
  }
}

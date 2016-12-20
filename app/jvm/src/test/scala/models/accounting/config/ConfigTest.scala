package models.accounting.config

import com.google.inject._
import common.testing._
import models._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class ConfigTest extends HookedSpecification {

  @Inject implicit val config: Config = null
  @Inject implicit val entityAccess: EntityAccess = null
  @Inject val userManager: User.Manager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test configuration parsing" in new WithApplication {
    // create test users
    val userA = userManager.add(SlickUserManager.createUser(loginName = "a", password = "pw", name = "Test User A"))
    val userB = userManager.add(SlickUserManager.createUser(loginName = "b", password = "pw", name = "Test User B"))
    val userOther = userManager.add(SlickUserManager.createUser(loginName = "other", password = "other", name = "Other"))

    // check keys
    config.accounts.keys.toList must beEqualTo(List("ACC_COMMON", "ACC_A", "ACC_B"))
    config.categories.keys.toList must beEqualTo(List("CAT_B", "CAT_A"))
    config.visibleReservoirs.map(_.code) must beEqualTo(List("CASH_COMMON", "CARD_COMMON", "CASH_A", "CARD_A", "CASH_B", "CARD_B"))

    // check content by samples
    config.accounts("ACC_A").code must beEqualTo("ACC_A")
    config.accounts("ACC_A").longName must beEqualTo("Account A")
    config.accounts("ACC_A").categories must beEqualTo(List(config.categories("CAT_A"), config.categories("CAT_B")))
    config.accounts("ACC_A").user mustEqual Some(userA)
    config.accounts("ACC_COMMON").user mustEqual None

    // check common account
    config.constants.commonAccount must beEqualTo(config.accounts("ACC_COMMON"))

    // check accountOf()
    config.accountOf(userA) must beEqualTo(Some(config.accounts("ACC_A")))
    config.accountOf(userB) must beEqualTo(Some(config.accounts("ACC_B")))
    config.accountOf(userOther) must beEqualTo(None)

    // test Account.isMineOrCommon()
    config.accounts("ACC_A").isMineOrCommon(userA, config, entityAccess) mustEqual true
    config.accounts("ACC_COMMON").isMineOrCommon(userA, config, entityAccess) mustEqual true
    config.accounts("ACC_B").isMineOrCommon(userA, config, entityAccess) mustEqual false
  }


  "test config.personallySortedAccounts()" in new WithApplication {

    // get vars
    val accCommon = config.constants.commonAccount
    val accA = config.accounts("ACC_A")
    val accB = config.accounts("ACC_B")
    val userA = userManager.add(SlickUserManager.createUser(loginName = "a", password = "a", name = "A"))
    // make sure all required users exist
    val userB = userManager.add(SlickUserManager.createUser(loginName = "b", password = "b", name = "B"))
    val userOther = userManager.add(SlickUserManager.createUser(loginName = "other", password = "other", name = "Other"))

    // call personallySortedAccounts()
    config.personallySortedAccounts(userA, entityAccess) mustEqual Seq(accCommon, accA, accB)
    config.personallySortedAccounts(userB, entityAccess) mustEqual Seq(accCommon, accB, accA)
    config.personallySortedAccounts(userOther, entityAccess) mustEqual Seq(accCommon, accA, accB)
  }
}

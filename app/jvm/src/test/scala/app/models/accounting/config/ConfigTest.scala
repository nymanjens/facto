package app.models.accounting.config

import app.common.testing.TestObjects._
import app.common.testing._
import app.models.access.JvmEntityAccess
import app.models.user.Users
import com.google.inject._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class ConfigTest extends HookedSpecification {

  @Inject implicit private val config: Config = null
  @Inject implicit private val entityAccess: JvmEntityAccess = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "configuration parsing" in new WithApplication {
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)

    // check keys
    config.accounts.keys.toList must beEqualTo(List("ACC_COMMON", "ACC_A", "ACC_B"))
    config.categories.keys.toList must beEqualTo(List("CAT_B", "CAT_A", "CAT_C"))
    config.visibleReservoirs.map(_.code) must beEqualTo(
      List("CASH_COMMON", "CARD_COMMON", "CASH_A", "CARD_A", "CASH_B", "CARD_B"))

    // check content by samples
    config.accounts("ACC_A").code must beEqualTo("ACC_A")
    config.accounts("ACC_A").longName must beEqualTo("Account A")
    config.accounts("ACC_A").categories must beEqualTo(
      List(config.categories("CAT_A"), config.categories("CAT_B")))
    config.accounts("ACC_A").user mustEqual Some(testUserA)
    config.accounts("ACC_COMMON").user mustEqual None

    // Check content is equal to testAccountingConfig
    config.accounts mustEqual testAccountingConfig.accounts
    config.categories mustEqual testAccountingConfig.categories
    config.moneyReservoirsMap mustEqual testAccountingConfig.moneyReservoirsMap
    config.templates mustEqual testAccountingConfig.templates
    config.constants mustEqual testAccountingConfig.constants
    config mustEqual testAccountingConfig
  }

  "config.commonAccount" in new WithApplication {
    config.constants.commonAccount must beEqualTo(config.accounts("ACC_COMMON"))
  }

  "config.accountOf()" in new WithApplication {
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)
    val userOther =
      TestUtils.persist(Users.createUser(loginName = "other", password = "other", name = "Other"))

    config.accountOf(testUserA) must beEqualTo(Some(config.accounts("ACC_A")))
    config.accountOf(testUserB) must beEqualTo(Some(config.accounts("ACC_B")))
    config.accountOf(userOther) must beEqualTo(None)
  }

  "config.isMineOrCommon()" in new WithApplication {
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)

    config.accounts("ACC_A").isMineOrCommon(testUserA, config, entityAccess) mustEqual true
    config.accounts("ACC_COMMON").isMineOrCommon(testUserA, config, entityAccess) mustEqual true
    config.accounts("ACC_B").isMineOrCommon(testUserA, config, entityAccess) mustEqual false
  }

  "config.personallySortedAccounts()" in new WithApplication {
    // get vars
    val accCommon = config.constants.commonAccount
    val accA = config.accounts("ACC_A")
    val accB = config.accounts("ACC_B")

    // make sure all required users exist
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)
    val userOther =
      TestUtils.persist(Users.createUser(loginName = "other", password = "other", name = "Other"))

    // call personallySortedAccounts()
    config.personallySortedAccounts(testUserA, entityAccess) mustEqual Seq(accCommon, accA, accB)
    config.personallySortedAccounts(testUserB, entityAccess) mustEqual Seq(accCommon, accB, accA)
    config.personallySortedAccounts(userOther, entityAccess) mustEqual Seq(accCommon, accA, accB)
  }
}

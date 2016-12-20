package models

import com.google.inject._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._

import models.SlickUtils.dbApi._
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models.SlickUtils.dbRun

@RunWith(classOf[JUnitRunner])
class SlickUserManagerTest extends HookedSpecification {

  @Inject val userManager: SlickUserManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the User model" in new WithApplication {

    val user1 = userManager.add(SlickUserManager.createUser(loginName = "alice", password = "j", name = "Alice"))
    userManager.fetchAll() mustEqual Seq(user1)

    userManager.authenticate(loginName = "alice", password = "j") mustEqual true
    userManager.authenticate(loginName = "wrong_username", password = "j") mustEqual false
    userManager.authenticate(loginName = "alice", password = "wrong password") mustEqual false

    userManager.findByLoginName(loginName = "alice") mustEqual Option(user1)
    userManager.findByLoginName(loginName = "wrong_username") mustEqual None
  }
}

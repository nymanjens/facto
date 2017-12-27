package models.user

import com.google.inject._
import common.testing._
import models.modification.EntityModification
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class SlickUserManagerTest extends HookedSpecification {

  @Inject private val userManager: SlickUserManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the User model" in new WithApplication {

    val user1 = SlickUserManager
      .createUser(loginName = "alice", password = "j", name = "Alice")
      .copy(idOption = Some(EntityModification.generateRandomId()))
    userManager.addIfNew(user1)
    userManager.fetchAll() mustEqual Seq(user1)

    userManager.authenticate(loginName = "alice", password = "j") mustEqual true
    userManager.authenticate(loginName = "wrong_username", password = "j") mustEqual false
    userManager.authenticate(loginName = "alice", password = "wrong password") mustEqual false

    userManager.findByLoginNameSync(loginName = "alice") mustEqual Option(user1)
    userManager.findByLoginNameSync(loginName = "wrong_username") mustEqual None
  }
}

package models.user

import com.google.inject._
import common.testing._
import models.access.JvmEntityAccess
import models.modification.EntityModification
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class UsersTest extends HookedSpecification {

  @Inject private val entityAccess: JvmEntityAccess = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "authenticate()" in new WithApplication {
    val user1 = Users
      .createUser(loginName = "alice", password = "j", name = "Alice")
      .copy(idOption = Some(EntityModification.generateRandomId()))
    entityAccess.persistEntityModifications(EntityModification.Add(user1))

    Users.authenticate(loginName = "alice", password = "j") mustEqual true
    Users.authenticate(loginName = "wrong_username", password = "j") mustEqual false
    Users.authenticate(loginName = "alice", password = "wrong password") mustEqual false
  }
}

package app.models.user

import app.common.testing._
import app.models.access.JvmEntityAccess
import com.google.inject._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class UsersTest extends HookedSpecification {

  @Inject implicit private val entityAccess: JvmEntityAccess = null
  @Inject implicit private val clock: FakeClock = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "createUser()" in new WithApplication {
    val user = Users.createUser("alice", password = "j", name = "Alice")

    user.loginName mustEqual "alice"
    user.name mustEqual "Alice"
  }

  "getOrCreateRobotUser()" in new WithApplication {
    val robotUser = Users.getOrCreateRobotUser()
    val secondRobotUser = Users.getOrCreateRobotUser()

    robotUser.loginName mustEqual "robot"
    secondRobotUser mustEqual robotUser
    entityAccess.newQuerySync[User]().data() mustEqual Seq(robotUser)
  }

  "authenticate()" in new WithApplication {
    TestUtils.persist(Users.createUser(loginName = "alice", password = "j", name = "Alice"))

    Users.authenticate(loginName = "alice", password = "j") mustEqual true
    Users.authenticate(loginName = "wrong_username", password = "j") mustEqual false
    Users.authenticate(loginName = "alice", password = "wrong password") mustEqual false
  }
}

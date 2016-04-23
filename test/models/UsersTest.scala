package models

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._

import slick.driver.H2Driver.api._
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.ModelUtils.dbRun

@RunWith(classOf[JUnitRunner])
class UserTest extends Specification {

  "test the User model" in new WithApplication(fakeApplication) {

    val user1 = Users.all.save(Users.newWithUnhashedPw(loginName = "alice", password = "j", name = "Alice"))
    dbRun(Users.all.result) mustEqual Seq(user1)

    Users.authenticate(loginName = "alice", password = "j") mustEqual true
    Users.authenticate(loginName = "wrong_username", password = "j") mustEqual false
    Users.authenticate(loginName = "alice", password = "wrong password") mustEqual false

    Users.findByLoginName(loginName = "alice") mustEqual Option(user1)
    Users.findByLoginName(loginName = "wrong_username") mustEqual None
  }
}

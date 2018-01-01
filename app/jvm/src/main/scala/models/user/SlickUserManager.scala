package models.user

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.time.Clock
import models.access.ModelField
import models.modification.EntityModification
import models.{EntityAccess, SlickEntityAccess}

import scala.util.Random

object SlickUserManager {

  def createUser(loginName: String, password: String, name: String): User =
    User(
      loginName = loginName,
      passwordHash = SlickUserManager.hash(password),
      name = name,
      databaseEncryptionKey = Random.alphanumeric.take(100).mkString(""),
      expandCashFlowTablesByDefault = true
    )

  def copyUserWithPassword(user: User, password: String): User = {
    user.copy(passwordHash = hash(password))
  }

  def getOrCreateRobotUser()(implicit entityAccess: SlickEntityAccess, clock: Clock): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString

    entityAccess.newQuerySyncForUser().findOne(ModelField.User.loginName, loginName) match {
      case Some(user) => user
      case None =>
        val userAddition = EntityModification.createAddWithRandomId(
          SlickUserManager.createUser(
            loginName = loginName,
            password = hash(clock.now.toString),
            name = "Robot"
          ))
        val userWithId = userAddition.entity
        entityAccess.persistEntityModifications(userAddition)(user = userWithId)
        userWithId
    }
  }

  def authenticate(loginName: String, password: String)(implicit entityAccess: EntityAccess): Boolean = {
    entityAccess.newQuerySyncForUser().findOne(ModelField.User.loginName, loginName) match {
      case Some(user) if user.passwordHash == hash(password) => true
      case _ => false
    }
  }

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString
}

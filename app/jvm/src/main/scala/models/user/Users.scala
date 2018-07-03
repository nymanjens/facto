package models.user

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.time.Clock
import models.access.{EntityAccess, JvmEntityAccess, ModelField}
import models.modification.EntityModification

import scala.util.Random

object Users {

  def createUser(loginName: String,
                 password: String,
                 name: String,
                 isAdmin: Boolean = false,
                 expandCashFlowTablesByDefault: Boolean = true,
                 expandLiquidationTablesByDefault: Boolean = true): User =
    User(
      loginName = loginName,
      passwordHash = hash(password),
      name = name,
      isAdmin = isAdmin,
      expandCashFlowTablesByDefault = expandCashFlowTablesByDefault,
      expandLiquidationTablesByDefault = expandLiquidationTablesByDefault
    )

  def copyUserWithPassword(user: User, password: String): User = {
    user.copy(passwordHash = hash(password))
  }

  def getOrCreateRobotUser()(implicit entityAccess: JvmEntityAccess, clock: Clock): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString

    entityAccess.newQuerySyncForUser().findOne(ModelField.User.loginName, loginName) match {
      case Some(user) => user
      case None =>
        val userAddition = EntityModification.createAddWithRandomId(
          createUser(
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
      case _                                                 => false
    }
  }

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString
}

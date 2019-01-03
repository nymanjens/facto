package app.models.user

import app.models.access.AppEntityAccess
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.modification.EntityModification
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import hydro.common.time.Clock
import hydro.models.access.DbQueryImplicits._

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

    entityAccess.newQuerySyncForUser().findOne(ModelFields.User.loginName === loginName) match {
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

  def authenticate(loginName: String, password: String)(implicit entityAccess: AppEntityAccess): Boolean = {
    entityAccess.newQuerySyncForUser().findOne(ModelFields.User.loginName === loginName) match {
      case Some(user) if user.passwordHash == hash(password) => true
      case _                                                 => false
    }
  }

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString
}

package models.user

import models.access.DbQueryImplicits._
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.time.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.access.ModelField
import models.manager.{ForwardingEntityManager, SlickEntityManager}
import models.modification.EntityModification
import models.user.SlickUserManager.{Users, tableName}
import models.{EntityAccess, EntityTable, SlickEntityAccess}

import scala.util.Random

final class SlickUserManager
    extends ForwardingEntityManager[User, Users](
      SlickEntityManager.create[User, Users](
        tag => new Users(tag),
        tableName = tableName
      ))
    with User.Manager {

  override def findByLoginNameSync(loginName: String): Option[User] = {
    val users: Seq[User] = dbRun(newQuery.filter(u => u.loginName === loginName).result)
    users match {
      case Seq() => None
      case Seq(u) => Option(u)
    }
  }
}

object SlickUserManager {
  private val tableName: String = "USERS"

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

    entityAccess.userManager.findByLoginNameSync(loginName) match {
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

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()

  final class Users(tag: Tag) extends EntityTable[User](tag, tableName) {
    def loginName = column[String]("loginName")

    def passwordHash = column[String]("passwordHash")

    def name = column[String]("name")

    def databaseEncryptionKey = column[String]("databaseEncryptionKey")

    def expandCashFlowTablesByDefault = column[Boolean]("expandCashFlowTablesByDefault")

    override def * =
      (loginName, passwordHash, name, databaseEncryptionKey, expandCashFlowTablesByDefault, id.?) <>
        (User.tupled, User.unapply)
  }
}

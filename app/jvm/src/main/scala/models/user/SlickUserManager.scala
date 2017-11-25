package models.user

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import models.EntityTable
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.manager.{ForwardingEntityManager, SlickEntityManager}
import models.user.SlickUserManager.{Users, tableName}

import scala.util.Random

final class SlickUserManager
    extends ForwardingEntityManager[User, Users](
      SlickEntityManager.create[User, Users](
        tag => new Users(tag),
        tableName = tableName
      ))
    with User.Manager {

  override def findByLoginName(loginName: String): Option[User] = {
    val users: Seq[User] = dbRun(newQuery.filter(u => u.loginName === loginName).result)
    users match {
      case Seq() => None
      case Seq(u) => Option(u)
    }
  }

  def authenticate(loginName: String, password: String): Boolean = {
    findByLoginName(loginName) match {
      case Some(user) if user.passwordHash == SlickUserManager.hash(password) => true
      case _ => false
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
      databaseEncryptionKey = Random.alphanumeric.take(100).mkString(""))

  def copyUserWithPassword(user: User, password: String): User = {
    user.copy(passwordHash = hash(password))
  }

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()

  final class Users(tag: Tag) extends EntityTable[User](tag, tableName) {
    def loginName = column[String]("loginName")

    def passwordHash = column[String]("passwordHash")

    def name = column[String]("name")

    def databaseEncryptionKey = column[String]("databaseEncryptionKey")

    override def * =
      (loginName, passwordHash, name, databaseEncryptionKey, id.?) <> (User.tupled, User.unapply)
  }
}

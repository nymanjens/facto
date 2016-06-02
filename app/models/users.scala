package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.manager.{EntityTable, ForwardingQueryableEntityManager, Identifiable, QueryableEntityManager}

case class User(loginName: String,
                passwordHash: String,
                name: String,
                idOption: Option[Long] = None) extends Identifiable[User] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def withPasswordHashFromUnhashed(password: String): User = {
    copy(passwordHash = Users.hash(password))
  }
}

class Users(tag: Tag) extends EntityTable[User](tag, Users.tableName) {
  def loginName = column[String]("loginName")

  def passwordHash = column[String]("passwordHash")

  def name = column[String]("name")

  override def * = (loginName, passwordHash, name, id.?) <>(User.tupled, User.unapply)
}

object Users extends ForwardingQueryableEntityManager[User, Users](
  QueryableEntityManager.backedByDatabase[User, Users](tag => new Users(tag), tableName = "USERS")) {

  private[models] def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()

  def newWithUnhashedPw(loginName: String, password: String, name: String) =
    new User(loginName, hash(password), name)

  def authenticate(loginName: String, password: String): Boolean = {
    val receivedUserList: Seq[User] = dbRun(newQuery.filter(u => u.loginName === loginName && u.passwordHash === hash(password)).result)
    receivedUserList match {
      case Seq() => false
      case Seq(u) => true
    }
  }

  def findByLoginName(loginName: String): Option[User] = {
    val receivedUserList: Seq[User] = dbRun(newQuery.filter(u => u.loginName === loginName).result)
    receivedUserList match {
      case Seq() => None
      case Seq(u) => Option(u)
    }
  }
}

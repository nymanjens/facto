package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import models.activeslick._
import models.SlickUtils.dbApi._
import SlickUtils.dbRun

case class User(loginName: String,
                passwordHash: String,
                name: String,
                id: Option[Long] = None) extends Identifiable[User] {

  override def withId(id: Long) = copy(id = Some(id))

  def withPasswordHashFromUnhashed(password: String): User = {
    copy(passwordHash = Users.hash(password))
  }
}

class Users(tag: Tag) extends EntityTable[User](tag, "USERS") {
  def loginName = column[String]("loginName")

  def passwordHash = column[String]("passwordHash")

  def name = column[String]("name")

  override def * = (loginName, passwordHash, name, id.?) <>(User.tupled, User.unapply)
}

object Users {
  val all = new EntityTableQuery[User, Users](tag => new Users(tag))

  private[models] def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()

  def newWithUnhashedPw(loginName: String, password: String, name: String) =
    new User(loginName, hash(password), name)

  def authenticate(loginName: String, password: String): Boolean = {
    val receivedUserList: Seq[User] = dbRun(Users.all.filter(u => u.loginName === loginName && u.passwordHash === hash(password)).result)
    receivedUserList match {
      case Seq() => false
      case Seq(u) => true
    }
  }

  def findByLoginName(loginName: String): Option[User] = {
    val receivedUserList: Seq[User] = dbRun(Users.all.filter(u => u.loginName === loginName).result)
    receivedUserList match {
      case Seq() => None
      case Seq(u) => Option(u)
    }
  }
}
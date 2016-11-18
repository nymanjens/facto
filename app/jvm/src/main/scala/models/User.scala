package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing

import models.manager.{Entity, EntityManager}

case class User(loginName: String,
                passwordHash: String,
                name: String,
                idOption: Option[Long] = None) extends Entity[User] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def withPasswordHashFromUnhashed(password: String): User = {
    copy(passwordHash = User.hash(password))
  }
}

object User {
  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()

  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[User] {

    def findByLoginName(loginName: String): Option[User]

    def newWithUnhashedPw(loginName: String, password: String, name: String): User =
      User(loginName, User.hash(password), name)

    def authenticate(loginName: String, password: String): Boolean = {
      findByLoginName(loginName) match {
        case Some(user) if user.passwordHash == User.hash(password) => true
        case _ => false
      }
    }
  }
}

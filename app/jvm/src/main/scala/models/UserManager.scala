package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing

import models.manager.{Entity, EntityManager}

trait UserManager extends EntityManager[User] {

  def findByLoginName(loginName: String): Option[User]

  def newWithUnhashedPw(loginName: String, password: String, name: String): User =
    User(loginName, UserManager.hash(password), name)

  def authenticate(loginName: String, password: String): Boolean = {
    findByLoginName(loginName) match {
      case Some(user) if user.passwordHash == UserManager.hash(password) => true
      case _ => false
    }
  }

}

object UserManager {
  private[models] def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()
}
package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing

import models.manager.Entity

case class User(loginName: String,
                passwordHash: String,
                name: String,
                idOption: Option[Long] = None) extends Entity[User] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def withPasswordHashFromUnhashed(password: String): User = {
    copy(passwordHash = UserManager.hash(password))
  }
}

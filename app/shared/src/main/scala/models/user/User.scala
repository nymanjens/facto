package models.user

import models.Entity
import models.manager.EntityManager

case class User(loginName: String,
                passwordHash: String,
                name: String,
                databaseEncryptionKey: String,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[User] {
    def findByLoginName(loginName: String): Option[User]
  }
}

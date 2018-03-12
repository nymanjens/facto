package models.user

import models.Entity

case class User(loginName: String,
                passwordHash: String,
                name: String,
                databaseEncryptionKey: String,
                expandCashFlowTablesByDefault: Boolean,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  def tupled = (this.apply _).tupled
}

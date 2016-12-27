package models

import api.ScalaJsApi.EntityType
import models.manager.{Entity, EntityManager}

case class User(loginName: String,
                passwordHash: String,
                name: String,
                idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  def tupled = (this.apply _).tupled

  implicit val entityType: EntityType[User] = EntityType.UserType

  trait Manager extends EntityManager[User] {
    def findByLoginName(loginName: String): Option[User]
  }
}

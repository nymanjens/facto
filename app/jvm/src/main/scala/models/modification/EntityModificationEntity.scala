package models.modification

import common.time.LocalDateTime
import models.user.User
import models.{Entity, EntityAccess}

/**
  * Symbolises a modification to an entity.
  *
  * EntityModificationEntity entities are immutable and are assumed to be relatively short-lived, especially after
  * code updates to related models.
  */
case class EntityModificationEntity(userId: Long,
                                    modification: EntityModification,
                                    date: LocalDateTime,
                                    idOption: Option[Long] = None)
    extends Entity {
  require(userId > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def user(implicit entityAccess: EntityAccess): User = entityAccess.newQuerySyncForUser().findById(userId)
}

object EntityModificationEntity {
  def tupled = (this.apply _).tupled
}

package hydro.api

import app.models.user.User
import hydro.models.modification.EntityModification
import hydro.models.Entity

/**
  * Permissions for arbitrary Entity read/write operations.
  *
  * Note that this explicitly excludes server-implemented modifications, which can have custom permissions.
  */
trait EntityPermissions {

  /** Throws an exception if this modifications is not allowed as write operation. */
  def checkAllowedForWrite(modification: EntityModification)(implicit user: User): Unit

  def isAllowedToRead(entity: Entity)(implicit user: User): Boolean

  final def isAllowedToStream(entityModification: EntityModification)(implicit user: User): Boolean = {
    entityModification match {
      case EntityModification.Add(entity)    => isAllowedToRead(entity)
      case EntityModification.Update(entity) => isAllowedToRead(entity)
      case EntityModification.Remove(_)      => true
    }
  }
}
object EntityPermissions {

  object DefaultImpl extends EntityPermissions {

    override def checkAllowedForWrite(modification: EntityModification)(implicit user: User): Unit = {
      require(modification.entityType != User.Type, "Please modify users by calling upsertUser() instead")
    }

    override def isAllowedToRead(entity: Entity)(implicit user: User): Boolean = true
  }
}

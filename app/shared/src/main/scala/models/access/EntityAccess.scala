package models.access

import models.Entity
import models.modification.EntityType
import models.user.User

trait EntityAccess {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  def newQuerySyncForUser(): DbResultSet.Sync[User]
}

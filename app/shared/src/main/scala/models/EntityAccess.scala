package models

import models.access.DbResultSet
import models.modification.EntityType
import models.user.User

trait EntityAccess {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  def newQuerySyncForUser(): DbResultSet.Sync[User]
}

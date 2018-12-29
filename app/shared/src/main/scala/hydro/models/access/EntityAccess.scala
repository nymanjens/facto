package hydro.models.access

import app.models.Entity
import app.models.modification.EntityType
import app.models.user.User

/** Central point of access to the storage layer. */
trait EntityAccess {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]
}

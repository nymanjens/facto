package hydro.models.access

import app.models.modification.EntityType
import hydro.models.Entity

/** Central point of access to the storage layer. */
trait EntityAccess {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]
}

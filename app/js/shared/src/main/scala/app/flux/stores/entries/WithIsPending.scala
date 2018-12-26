package app.flux.stores.entries

import models.Entity
import models.access.JsEntityAccess
import models.modification.EntityType

case class WithIsPending[Entry](entry: Entry, isPending: Boolean)

object WithIsPending {

  /** Shortcut to cut down on boiler plate. */
  def isPending[E <: Entity: EntityType](entity: E)(implicit entityAccess: JsEntityAccess): Boolean =
    entityAccess.pendingModifications.additionIsPending(entity)

  /** Shortcut to cut down on boiler plate. */
  def isAnyPending[E <: Entity: EntityType](entities: Iterable[E])(
      implicit entityAccess: JsEntityAccess): Boolean =
    entities.exists(isPending)
}

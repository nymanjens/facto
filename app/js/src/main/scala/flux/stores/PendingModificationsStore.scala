package flux.stores

import flux.stores.PendingModificationsStore.State
import models.access.JsEntityAccess
import models.modification.EntityModification

import scala.collection.immutable.Seq

final class PendingModificationsStore(implicit jsEntityAccess: JsEntityAccess) extends StateStore[State] {
  jsEntityAccess.registerListener(JsEntityAccessListener)

  private var _state: State = State(numberOfModifications = 0)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private state helper methods ****************//
  private def setState(state: State): Unit = {
    val originalState = _state
    _state = state
    if (_state != originalState) {
      invokeStateUpdateListeners()
    }
  }

  // **************** Private inner types ****************//
  object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      if (jsEntityAccess.pendingModifications.persistedLocally) {
        setState(State(numberOfModifications = jsEntityAccess.pendingModifications.size))
      } else {
        setState(State(numberOfModifications = 0))
      }
    }
  }
}
object PendingModificationsStore {

  /** numberOfModifications: Number of pending modifications that have been persisted locally. */
  case class State(numberOfModifications: Int)
}

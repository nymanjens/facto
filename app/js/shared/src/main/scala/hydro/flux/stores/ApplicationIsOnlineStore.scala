package hydro.flux.stores

import hydro.common.Listenable
import hydro.flux.stores.ApplicationIsOnlineStore.State
import app.models.access.EntityModificationPushClientFactory

final class ApplicationIsOnlineStore(
    implicit entityModificationPushClientFactory: EntityModificationPushClientFactory)
    extends StateStore[State] {

  entityModificationPushClientFactory.pushClientsAreOnline.registerListener(PushClientsAreOnlineListener)

  private var _state: State = State(isOnline = entityModificationPushClientFactory.pushClientsAreOnline.get)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private inner types ****************//
  object PushClientsAreOnlineListener extends Listenable.Listener[Boolean] {
    override def onChange(isOnline: Boolean): Unit = {
      _state = State(isOnline)
      invokeStateUpdateListeners()
    }
  }
}

object ApplicationIsOnlineStore {
  case class State(isOnline: Boolean)
}

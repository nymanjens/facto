package hydro.flux.stores

import hydro.common.Listenable
import hydro.flux.stores.LocalDatabaseHasBeenLoadedStore.State
import hydro.models.access.JsEntityAccess

final class LocalDatabaseHasBeenLoadedStore(implicit jsEntityAccess: JsEntityAccess)
    extends StateStore[State] {

  jsEntityAccess.localDatabaseHasBeenLoaded.registerListener(HasBeenLoadedListener)

  private var _state: State = State(hasBeenLoaded = jsEntityAccess.localDatabaseHasBeenLoaded.get)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private inner types ****************//
  object HasBeenLoadedListener extends Listenable.Listener[Boolean] {
    override def onChange(hasBeenLoaded: Boolean): Unit = {
      _state = State(hasBeenLoaded = hasBeenLoaded)
      invokeStateUpdateListeners()
    }
  }
}

object LocalDatabaseHasBeenLoadedStore {
  case class State(hasBeenLoaded: Boolean)
}

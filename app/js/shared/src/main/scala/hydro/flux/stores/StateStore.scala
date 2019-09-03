package hydro.flux.stores

import scala.collection.mutable

/**
  * Abstract base class for any store that exposes a single listenable state.
  *
  * @tparam State An immutable type that contains all state maintained by this store
  */
abstract class StateStore[State] {

  private val _stateUpdateListeners: mutable.Set[StateStore.Listener] = mutable.LinkedHashSet()
  private var isCallingListeners: Boolean = false

  // **************** Public API: To override ****************//
  def state: State

  // **************** Public API: Final ****************//
  final def register(listener: StateStore.Listener): Unit = {
    checkNotCallingListeners()

    _stateUpdateListeners.add(listener)
    onStateUpdateListenersChange()
  }

  final def deregister(listener: StateStore.Listener): Unit = {
    checkNotCallingListeners()

    _stateUpdateListeners.remove(listener)
    onStateUpdateListenersChange()
  }

  // **************** Protected methods to override ****************//
  protected def onStateUpdateListenersChange(): Unit = {}

  // **************** Protected helper methods ****************//
  protected final def invokeStateUpdateListeners(): Unit = {
    checkNotCallingListeners()
    isCallingListeners = true
    _stateUpdateListeners.foreach(_.onStateUpdate())
    isCallingListeners = false
  }

  final def stateUpdateListeners: scala.collection.Set[StateStore.Listener] = _stateUpdateListeners

  protected final def checkNotCallingListeners(): Unit = {
    require(!isCallingListeners, "checkNotCallingListeners(): But isCallingListeners is true")
  }
}

object StateStore {

  def alwaysReturning[State](fixedState: State): StateStore[State] = new StateStore[State] {
    override def state: State = fixedState
  }

  trait Listener {
    def onStateUpdate(): Unit
  }
}

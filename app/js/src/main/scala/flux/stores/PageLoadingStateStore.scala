package flux.stores

import flux.action.{Action, Dispatcher}
import flux.stores.PageLoadingStateStore.State

import scala.collection.immutable.Seq

final class PageLoadingStateStore(implicit dispatcher: Dispatcher) {
  dispatcher.registerPartialSync(dispatcherListener)

  private var _state: State = State(isLoading = false)

  private var stateUpdateListeners: Seq[PageLoadingStateStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  def state: State = _state

  def register(listener: PageLoadingStateStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  def deregister(listener: PageLoadingStateStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Private dispatcher methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case Action.SetPageLoadingState(isLoading) =>
      setState(State(isLoading = isLoading))
  }

  // **************** Private state helper methods ****************//
  private def setState(state: State): Unit = {
    val originalState = _state
    _state = state
    if (_state != originalState) {
      invokeListeners()
    }
  }

  private def invokeListeners(): Unit = {
    require(!isCallingListeners)
    isCallingListeners = true
    stateUpdateListeners.foreach(_.onStateUpdate())
    isCallingListeners = false
  }
}

object PageLoadingStateStore {
  case class State(isLoading: Boolean)

  trait Listener {
    def onStateUpdate(): Unit
  }
}

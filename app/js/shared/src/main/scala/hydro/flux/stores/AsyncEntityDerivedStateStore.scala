package hydro.flux.stores

import hydro.models.modification.EntityModification
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.logFailure
import hydro.models.access.JsEntityAccess

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Abstract base class for any store that exposes a single listenable state and calculates the new value of the state
  * in an async manner from `JsEntityAccess`.
  *
  * @tparam State An immutable type that contains all state maintained by this store
  */
abstract class AsyncEntityDerivedStateStore[State](implicit entityAccess: JsEntityAccess)
    extends StateStore[Option[State]] {
  entityAccess.registerListener(JsEntityAccessListener)

  private var _state: Option[State] = None
  private var stateIsStale: Boolean = true
  private var stateUpdateInFlight: Boolean = false
  private val staleBecomesFalseListeners: mutable.Set[StateStore.Listener] = mutable.LinkedHashSet()

  /** Buffer of modifications that were added during the last update. */
  private var pendingModifications: Seq[EntityModification] = Seq()

  // **************** Public API ****************//
  override final def state: Option[State] = _state

  /** Returns a future that is resolved as soon as `this.state` has a non-stale value. */
  final def stateFuture: Future[State] = state match {
    case Some(s) if !stateIsStale => Future.successful(s)
    case _ =>
      val promise = Promise[State]()
      val listener: StateStore.Listener = () => {
        if (state.isDefined && !promise.isCompleted && !stateIsStale) {
          promise.success(state.get)
        }
      }
      // Note: Regular State change listener is required because registration triggers recalculation if there are
      // no other listeners.
      // The StaleBecomesFalseListener is required because the regular listener doesn't get triggered if there is
      // no state change (even if it went from stale to non-stale).
      register(listener)
      registerStaleBecomesFalseListener(listener)
      promise.future.map(_ => deregister(listener))
      promise.future.map(_ => deregisterStaleBecomesFalseListener(listener))
      promise.future
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): Future[State]

  protected def modificationImpactsState(entityModification: EntityModification, state: State): Boolean

  // **************** StateStore hooks ****************//
  override protected final def onStateUpdateListenersChange(): Unit = {
    if (stateUpdateListeners.nonEmpty && stateIsStale && !stateUpdateInFlight) {
      startStateUpdate()
    }
  }

  // **************** Private helper methods ****************//
  private def registerStaleBecomesFalseListener(listener: StateStore.Listener): Unit = {
    staleBecomesFalseListeners.add(listener)
  }

  private def deregisterStaleBecomesFalseListener(listener: StateStore.Listener): Unit = {
    staleBecomesFalseListeners.remove(listener)
  }

  private def startStateUpdate(): Unit = logExceptions {
    require(stateIsStale, "State is not stale while starting state update")
    require(stateUpdateListeners.nonEmpty, "Nobody is listening to the state, so why update it?")
    require(!stateUpdateInFlight, "A state update is already in flight. This is not supported.")

    stateUpdateInFlight = true
    logFailure {
      calculateState().map { calculatedState =>
        stateUpdateInFlight = false

        if (impactsState(pendingModifications, calculatedState)) {
          // Relevant modifications were added since the start of calculation -> recalculate
          pendingModifications = Seq()
          if (stateUpdateListeners.nonEmpty) {
            startStateUpdate()
          }
        } else {
          pendingModifications = Seq()
          stateIsStale = false
          if (_state != Some(calculatedState)) {
            _state = Some(calculatedState)
            invokeStateUpdateListeners()
          }
          staleBecomesFalseListeners.foreach(_.onStateUpdate())
        }
      }
    }
  }

  private def impactsState(modifications: Seq[EntityModification], state: State): Boolean =
    modifications.toStream.filter(m => modificationImpactsState(m, state)).take(1).nonEmpty

  // **************** Inner type definitions ****************//
  private object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      checkNotCallingListeners()

      val modificationsCouldBeRelevant = _state match {
        case None    => true
        case Some(s) => impactsState(modifications, s)
      }
      if (modificationsCouldBeRelevant) {
        if (stateIsStale) {
          if (stateUpdateListeners.nonEmpty) {
            require(stateUpdateInFlight, s"Expected stateUpdateInFlight = true")
            pendingModifications = pendingModifications ++ modifications
          }
        } else {
          require(!stateUpdateInFlight, "stateUpdateInFlight is true but stateIsStale is false")
          stateIsStale = true

          if (stateUpdateListeners.nonEmpty) {
            startStateUpdate()
          }
        }
      }
    }
  }
}

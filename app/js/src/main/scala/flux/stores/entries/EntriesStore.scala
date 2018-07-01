package flux.stores.entries

import common.LoggingUtils.logFailure
import flux.stores.StateStore
import models.access.JsEntityAccess
import models.accounting.{BalanceCheck, Transaction}
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * General purpose flux store that maintains a state derived from data in the database
  * and doesn't support mutation operations.
  *
  * @tparam State Any immutable type that contains all state maintained by this store
  */
abstract class EntriesStore[State <: EntriesStore.StateTrait](implicit entityAccess: JsEntityAccess)
    extends StateStore[Option[State]] {
  entityAccess.registerListener(JsEntityAccessListener)

  private var _state: Option[State] = None
  private var stateIsStale: Boolean = true
  private var stateUpdateInFlight: Boolean = false

  /** Buffer of modifications that were added during the last update. */
  private var pendingModifications: Seq[EntityModification] = Seq()

  // **************** Public API ****************//
  override final def state: Option[State] = _state

  /** Returns a future that is resolved as soon as `this.state` has a non-stale value. */
  final def stateFuture: Future[State] = state match {
    case Some(s) => Future.successful(s)
    case None =>
      val promise = Promise[State]()
      val listener: StateStore.Listener = () => {
        if (state.isDefined && !promise.isCompleted) {
          promise.success(state.get)
        }
      }
      register(listener)
      promise.future.map(_ => deregister(listener))
      promise.future
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): Future[State]

  protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean
  protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State): Boolean

  // **************** StateStore hooks ****************//
  override protected final def onStateUpdateListenersChange(): Unit = {
    if (stateUpdateListeners.nonEmpty && stateIsStale && !stateUpdateInFlight) {
      startStateUpdate()
    }
  }

  // **************** Private helper methods ****************//
  private def startStateUpdate(): Unit = {
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
        }
      }
    }
  }

  private def impactsState(modifications: Seq[EntityModification], state: State): Boolean =
    modifications.toStream.filter(m => modificationImpactsState(m, state)).take(1).nonEmpty

  private def modificationImpactsState(entityModification: EntityModification, state: State): Boolean = {
    entityModification.entityType match {
      case EntityType.UserType => true // Almost never happens and likely to change entries
      case EntityType.ExchangeRateMeasurementType =>
        false // In normal circumstances, no entries should be changed retroactively
      case EntityType.TransactionGroupType =>
        entityModification match {
          case EntityModification.Add(_)    => false // Always gets added alongside Transaction additions
          case EntityModification.Update(_) => throw new UnsupportedOperationException("Immutable entity")
          case EntityModification.Remove(_) => false // Always gets removed alongside Transaction removals
        }
      case EntityType.TransactionType =>
        entityModification match {
          case EntityModification.Add(transaction) =>
            transactionUpsertImpactsState(transaction.asInstanceOf[Transaction], state)
          case EntityModification.Update(_) => throw new UnsupportedOperationException("Immutable entity")
          case EntityModification.Remove(transactionId) =>
            state.impactedByTransactionId(transactionId)
        }
      case EntityType.BalanceCheckType =>
        entityModification match {
          case EntityModification.Add(bc) =>
            balanceCheckUpsertImpactsState(bc.asInstanceOf[BalanceCheck], state)
          case EntityModification.Update(_) => throw new UnsupportedOperationException("Immutable entity")
          case EntityModification.Remove(bcId) =>
            state.impactedByBalanceCheckId(bcId)
        }
    }
  }

  // **************** Inner type definitions ****************//
  private object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      checkNotCallingListeners()

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

object EntriesStore {

  trait StateTrait {
    protected def impactingTransactionIds: Set[Long]
    protected def impactingBalanceCheckIds: Set[Long]

    private[entries] final def impactedByTransactionId(id: Long): Boolean =
      impactingTransactionIds contains id
    private[entries] final def impactedByBalanceCheckId(id: Long): Boolean =
      impactingBalanceCheckIds contains id
  }
}

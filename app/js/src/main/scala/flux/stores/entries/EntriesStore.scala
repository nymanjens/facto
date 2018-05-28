package flux.stores.entries

import common.LoggingUtils
import common.LoggingUtils.logFailure
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
abstract class EntriesStore[State <: EntriesStore.StateTrait](implicit entityAccess: JsEntityAccess) {
  entityAccess.registerListener(JsEntityAccessListener)

  private var _state: Option[State] = None
  private var stateUpdateInFlight: Boolean = false

  /** Buffer of modifications that were added during the last update. */
  private var pendingModifications: Seq[EntityModification] = Seq()

  private var stateUpdateListeners: Seq[EntriesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: Option[State] = _state

  /** Returns a future that is resolved as soon as `this.state` has a non-stale value. */
  final lazy val stateFuture: Future[State] = _state match {
    case Some(s) => Future.successful(s)
    case None =>
      val promise = Promise[State]()
      val listener: EntriesStore.Listener = () => {
        if (state.isDefined && !promise.isCompleted) {
          promise.success(state.get)
        }
      }
      register(listener)
      promise.future.map(_ => deregister(listener))
      promise.future
  }

  final def register(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
    if (_state.isEmpty && !stateUpdateInFlight) {
      startStateUpdate()
    }
  }

  final def deregister(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): Future[State]

  protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean
  protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State): Boolean

  // **************** Private helper methods ****************//
  private def startStateUpdate(): Unit = {
    require(_state.isEmpty, "State is not empty while starting state update")
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
        } else if (_state != Some(calculatedState)) {
          _state = Some(calculatedState)
          invokeListeners()
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

  private def invokeListeners(): Unit = {
    require(!isCallingListeners)
    isCallingListeners = true
    stateUpdateListeners.foreach(_.onStateUpdate())
    isCallingListeners = false
  }

  // **************** Inner type definitions ****************//
  private object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)

      _state match {
        case Some(s) if impactsState(modifications, s) =>
          require(!stateUpdateInFlight, "stateUpdateInFlight is true but _state is non empty")
          _state = None

          if (stateUpdateListeners.nonEmpty) {
            startStateUpdate()
          }

        case None if stateUpdateListeners.nonEmpty =>
          require(stateUpdateInFlight, s"Expected stateUpdateInFlight = true")
          pendingModifications = pendingModifications ++ modifications

        case _ =>
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

  trait Listener {
    def onStateUpdate(): Unit
  }
}

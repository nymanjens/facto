package flux.stores.entries

import flux.stores.entries.EntriesStore.StateWithMeta
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
abstract class EntriesStore[State <: EntriesStore.StateTrait](implicit database: JsEntityAccess) {
  database.registerListener(RemoteDatabaseProxyListener)

  private var _state: Option[State] = None
  private var stateVersion: Int = 1
  private var stateVersionInFlight: Int = stateVersion
  private var pendingModifications: Seq[EntityModification] = Seq()

  private var stateUpdateListeners: Seq[EntriesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: StateWithMeta[State] = _state match {
    case None    => StateWithMeta.Empty()
    case Some(s) => StateWithMeta.WithValue(s, isStale = stateUpdateInFlight)
  }

  final def stateFuture: Future[State] = _state match {
    case Some(s) => Future.successful(s)
    case None =>
      val promise = Promise[State]()
      val listener: EntriesStore.Listener = () => {
        if (_state.isDefined) {
          promise.success(_state.get)
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
  private def stateUpdateInFlight: Boolean = stateVersion != stateVersionInFlight

  private def startStateUpdate(): Unit = {
    stateVersionInFlight += 1
    val newStateVersion = stateVersionInFlight
    calculateState().map { calculatedState =>
      if (stateVersion >= newStateVersion) {
        // This response is no longer relevant, do nothing
      } else {
        stateVersion = newStateVersion
        if (_state != Some(calculatedState)) {
          _state = Some(calculatedState)
          invokeListeners()
        }
        revisitPendingModifications()
      }
    }
  }

  private def revisitPendingModifications(): Unit = {
    if (!stateUpdateInFlight) {
      if (_state.isDefined) {
        if (impactsState(pendingModifications, _state.get)) {
          startStateUpdate()
        }
      }
      pendingModifications = Seq()
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
  private object RemoteDatabaseProxyListener extends JsEntityAccess.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    override def localModificationPersistedRemotely(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)

      if (_state.isDefined) {
        if (stateUpdateListeners.nonEmpty) {
          if (impactsState(modifications, _state.get)) {
            invokeListeners()
          }
        }
      }
    }

    override def addedRemotely(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    private def addedModifications(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)

      if (_state.isDefined) {
        if (stateUpdateListeners.nonEmpty) {
          pendingModifications = pendingModifications ++ modifications
          revisitPendingModifications()
        } else { // No listeners
          if (impactsState(modifications, _state.get)) {
            _state = None
          }
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

  trait Listener {
    def onStateUpdate(): Unit
  }

  sealed trait StateWithMeta[State] {
    def hasState: Boolean
    def state: State
    def isStale: Boolean

    final def stateOption: Option[State] = if (hasState) Some(state) else None
  }
  object StateWithMeta {
    case class Empty[State]() extends StateWithMeta[State] {
      override def hasState = false
      override def state = throw new IllegalArgumentException("Empty state")
      override def isStale = true
    }
    case class WithValue[State](override val state: State, override val isStale: Boolean)
        extends StateWithMeta[State] {
      override def hasState = true
    }
  }
}

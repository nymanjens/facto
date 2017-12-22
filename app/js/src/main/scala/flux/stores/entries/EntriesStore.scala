package flux.stores.entries

import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq

/**
  * General purpose flux store that maintains a state derived from data in the `RemoteDatabaseProxy`
  * and doesn't support mutation operations.
  *
  * @tparam State Any immutable type that contains all state maintained by this store
  */
abstract class EntriesStore[State <: EntriesStore.StateTrait](implicit database: RemoteDatabaseProxy) {
  database.registerListener(RemoteDatabaseProxyListener)

  private var _state: Option[State] = None
  private var stateUpdateListeners: Seq[EntriesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: State = {
    if (_state.isEmpty) {
      updateState()
    }

    _state.get
  }

  final def register(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  final def deregister(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): State

  protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean
  protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State): Boolean

  // **************** Private helper methods ****************//
  private def updateState(): Unit = {
    _state = Some(calculateState())
  }

  private def impactsState(modifications: Seq[EntityModification]): Boolean =
    modifications.toStream.filter(m => modificationImpactsState(m, state)).take(1).nonEmpty

  private def modificationImpactsState(entityModification: EntityModification, state: State): Boolean = {
    entityModification.entityType match {
      case EntityType.UserType => true // Almost never happens and likely to change entries
      case EntityType.ExchangeRateMeasurementType =>
        false // In normal circumstances, no entries should be changed retroactively
      case EntityType.TransactionGroupType =>
        entityModification match {
          case EntityModification.Add(_) => false // Always gets added alongside Transaction additions
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
  private object RemoteDatabaseProxyListener extends RemoteDatabaseProxy.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    override def localModificationPersistedRemotely(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)

      if (_state.isDefined) {
        if (stateUpdateListeners.nonEmpty) {
          if (impactsState(modifications)) {
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
        if (impactsState(modifications)) {
          if (stateUpdateListeners.isEmpty) {
            _state = None
          } else {
            updateState()
            invokeListeners()
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
}

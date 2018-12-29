package app.flux.stores.entries

import hydro.flux.stores.AsyncEntityDerivedStateStore
import app.models.access.AppJsEntityAccess
import app.models.access.JsEntityAccess
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.modification.EntityModification
import app.models.modification.EntityType

/**
  * Abstract base class for a store with transaction-derived entries.
  *
  * @tparam State An immutable type that contains all state maintained by this store
  */
abstract class EntriesStore[State <: EntriesStore.StateTrait](implicit entityAccess: AppJsEntityAccess)
    extends AsyncEntityDerivedStateStore[State] {

  // **************** Abstract methods ****************//
  protected def transactionUpsertImpactsState(transaction: Transaction, state: State): Boolean
  protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State): Boolean

  // **************** Implementation of abstract methods ****************//
  protected override def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean = {
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

package app.flux.stores.entries.factories

import app.flux.stores.entries.WithIsPending.isAnyPending
import app.flux.stores.entries._
import app.models.access.AppJsEntityAccess

import scala.collection.immutable.Seq

/**
 * @tparam AdditionalInput The (immutable) input type that together with injected dependencies and the max number of
 *                         entries is enough to calculate the latest value of `State`. Example: Int.
 */
abstract class EntriesListStoreFactory[Entry, AdditionalInput](implicit entityAccess: AppJsEntityAccess)
    extends EntriesStoreFactory[EntriesListStoreFactory.State[Entry]] {

  // **************** Abstract methods/types ****************//
  protected def createNew(maxNumEntries: Int, input: AdditionalInput): Store

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected final def createNew(input: Input) = {
    require(input.maxNumEntries > 0, s"maxNumEntries must be positive but was ${input.maxNumEntries}")
    createNew(input.maxNumEntries, input.additionalInput)
  }

  /* override */
  case class Input(maxNumEntries: Int, additionalInput: AdditionalInput)
}

object EntriesListStoreFactory {

  /**
   * @param entries the latest `maxNumEntries` entries sorted from old to new.
   */
  case class State[Entry](
      entries: Seq[WithIsPending[Entry]],
      hasMore: Boolean,
      override val impactingTransactionIds: Set[Long],
      override val impactingBalanceCheckIds: Set[Long],
  ) extends EntriesStore.StateTrait {
    def isEmpty: Boolean = entries.isEmpty
  }
  object State {
    def empty[Entry]: State[Entry] =
      State(Seq(), hasMore = false, impactingTransactionIds = Set(), impactingBalanceCheckIds = Set())

    def withImpactingIdsInEntries[Entry <: GroupedTransactions](entries: Seq[Entry], hasMore: Boolean)(
        implicit entityAccess: AppJsEntityAccess
    ): State[Entry] =
      State(
        entries.map(entry => WithIsPending(entry, isPending = isAnyPending(entry.transactions))),
        hasMore = hasMore,
        impactingTransactionIds = entries.toStream.flatMap(_.transactions).map(_.id).toSet,
        impactingBalanceCheckIds = Set(),
      )
  }
}

package flux.stores

import models.access.RemoteDatabaseProxy

import scala.collection.immutable.Seq

/**
  * @tparam AdditionalInput The (immutable) input type that together with injected dependencies and the max number of
  *                         entries is enough to calculate the latest value of `State`. Example: Int.
  */
abstract class EntriesStoreListFactory[Entry, AdditionalInput](implicit database: RemoteDatabaseProxy)
  extends EntriesStoreFactory[EntriesStoreListFactory.State[Entry]] {

  // **************** Abstract methods/types ****************//
  protected def createNew(maxNumEntries: Int, input: AdditionalInput): Store

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected final def createNew(input: Input) = createNew(input.maxNumEntries, input.additionalInput)

  /* override */ case class Input(maxNumEntries: Int, additionalInput: AdditionalInput)
}

object EntriesStoreListFactory {

  /**
    * @param entries the latest `maxNumEntries` entries sorted from old to new.
    */
  case class State[Entry](entries: Seq[Entry], hasMore: Boolean)
  object State {
    def empty[Entry]: State[Entry] = State(Seq(), false)
  }
}

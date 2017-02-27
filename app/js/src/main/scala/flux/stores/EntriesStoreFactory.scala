package flux.stores

import scala.collection.mutable

private[stores] abstract class EntriesStoreFactory[StateT] {

  private val cache: mutable.Map[Input, Store] = mutable.Map()

  // **************** Abstract methods/types ****************//
  /**
    * The (immutable) input type that together with injected dependencies is enough to
    * calculate the latest value of `State`. Example: Int.
    */
  protected type Input

  protected def createNew(input: Input): Store

  // **************** API ****************//
  /** Implementing classes could add a specialized version of `get()` with unpacked parameters. */
  final def get(input: Input): Store = {
    if (cache contains input) {
      cache(input)
    } else {
      val created = createNew(input)
      cache.put(input, created)
      created
    }
  }

  // **************** Type aliases for brevity ****************//
  final type State = StateT
  final type Store = EntriesStore[StateT]
}

package hydro.flux.stores

import scala.collection.mutable

abstract class StoreFactory {

  private val cache: mutable.Map[Input, Store] = mutable.Map()

  // **************** Abstract methods/types ****************//
  /**
   * The (immutable) input type that together with injected dependencies is enough to
   * calculate the latest value of `State`. Example: Int.
   */
  protected type Input

  /** The type of store that gets created by this factory. */
  protected type Store

  protected def createNew(input: Input): Store

  // **************** Protected API ****************//
  protected final def getCachedOrCreate(input: Input): Store = {
    if (cache contains input) {
      cache(input)
    } else {
      val created = createNew(input)
      cache.put(input, created)
      created
    }
  }
}

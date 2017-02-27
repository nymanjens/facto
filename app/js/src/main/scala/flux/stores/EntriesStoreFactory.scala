package flux.stores

import scala.collection.mutable

private[stores] abstract class EntriesStoreFactory[StateT] {
  /**
    * The (immutable) input type that together with injected dependencies is enough to
    * calculate the latest value of `State`. Example: Int.
    */
  protected type Input

  private val cache: mutable.Map[Input, Store] = mutable.Map()

  final def get(input: Input): Store = {
    if (cache contains input) {
      cache(input)
    } else {
      val created = createNew(input)
      cache.put(input, created)
      created
    }
  }

  protected def createNew(input: Input): Store

  // Type aliases for brevity
  final type State = StateT
  final type Store = EntriesStore[StateT]
}

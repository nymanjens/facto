package stores

import scala.collection.mutable

private[stores] abstract class EntriesStoreFactory[State] {
  /** COM */
  protected type Input

  private val cache: mutable.Map[Input, EntriesStore[State]] = mutable.Map()

  final def get(input: Input): EntriesStore[State] = {
    if (cache contains input) {
      cache(input)
    } else {
      val created = createNew(input)
      cache.put(input, created)
      created
    }
  }

  protected def createNew(input: Input): EntriesStore[State]
}

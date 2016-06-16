package common.cache

import com.google.common.hash.{Funnel, HashCode, PrimitiveSink}

trait UniquelyHashable extends Object {

  /**
    * Returns a Guava HashCode that uniquely defines this object when compared to other instances of the same type.
    *
    * Note that this may be a heuristic if the chance of collisions is very low.
    */
  def uniqueHash: HashCode
}

object UniquelyHashable {

  /**
    * A Guava funnel for a sequence of uniquely hashable objects.
    *
    * This can be used as follows:
    *
    * Hashing.sha1().newHasher()
    * .putObject(hashables, UniquelyHashableIterableFunnel)
    * .hash()
    */
  object UniquelyHashableIterableFunnel extends Funnel[Iterable[UniquelyHashable]] {
    override def funnel(from: Iterable[UniquelyHashable], into: PrimitiveSink): Unit = {
      for (hashable <- from) {
        into.putBytes(hashable.uniqueHash.asBytes)
      }
    }
  }
}
package common.cache

import com.google.common.hash.{Funnel, HashCode, PrimitiveSink}

trait UniquelyHashable extends Object {
  def uniqueHash: HashCode
}

object UniquelyHashable {

  object UniquelyHashableIterableFunnel extends Funnel[Iterable[UniquelyHashable]] {
    override def funnel(from: Iterable[UniquelyHashable], into: PrimitiveSink): Unit = {
      for(hashable <- from) {
        into.putBytes(hashable.uniqueHash.asBytes)
      }
    }
  }
}
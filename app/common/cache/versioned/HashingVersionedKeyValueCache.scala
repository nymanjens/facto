package common.cache.versioned

import com.google.common.hash.HashCode
import common.cache.UniquelyHashable

/**
  * VersionedKeyValueCache decorator that stores the unique hashes of the keys and versions, rather than the instances
  * themselves. This can be useful when the keys or versions are expensive to compare or to keep in memory.
  */
private[versioned] final class HashingVersionedKeyValueCache[Key <: UniquelyHashable, Version <: UniquelyHashable,
Value](delegate: VersionedKeyValueCache[HashCode, HashCode, Value])
  extends VersionedKeyValueCache[Key, Version, Value] {

  override def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value =
    delegate.getOrCalculate(key.uniqueHash, version.uniqueHash, calculateValueFunc)
}

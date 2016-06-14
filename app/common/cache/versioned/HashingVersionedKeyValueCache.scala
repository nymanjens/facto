package common.cache.versioned

import com.google.common.hash.HashCode
import common.cache.UniquelyHashable

private[versioned] final class HashingVersionedKeyValueCache[Key <: UniquelyHashable, Version <: UniquelyHashable,
Value](delegate: VersionedKeyValueCache[HashCode, HashCode, Value])
  extends VersionedKeyValueCache[Key, Version, Value] {

  override def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value =
    delegate.getOrCalculate(key.uniqueHash, version.uniqueHash, calculateValueFunc)
}

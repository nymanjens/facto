package common.cache.versioned

import com.google.common.hash.HashCode
import common.cache.UniquelyHashable

trait VersionedKeyValueCache[Key, Version, Value] {

  def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value
}

object VersionedKeyValueCache {

  def apply[Key, Version, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new VersionedKeyValueCacheImpl[Key, Version, Value]()

  def hashingKeys[Key <: UniquelyHashable, Version <: UniquelyHashable, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new HashingVersionedKeyValueCache(new VersionedKeyValueCacheImpl[HashCode, HashCode, Value]())
}

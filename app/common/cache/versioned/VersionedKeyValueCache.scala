package common.cache.versioned

import com.google.common.hash.HashCode
import common.cache.UniquelyHashable

/** Defines a cache that only keeps the latest version of a key. */
trait VersionedKeyValueCache[Key, Version, Value] {

  /**
    * If this key and version are cached, returns the cached value. Otherwise calculates the value from the given
    * function and stores it in the cache, replacing any earlier version for this key in the process.
    */
  def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value
}

object VersionedKeyValueCache {

  def apply[Key, Version, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new VersionedKeyValueCacheImpl[Key, Version, Value]()

  /**
    * Creates a VersionedKeyValueCache that stores the unique hashes of the keys and versions, rather than the instances
    * themselves.
    */
  def hashingKeys[Key <: UniquelyHashable, Version <: UniquelyHashable, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new HashingVersionedKeyValueCache(new VersionedKeyValueCacheImpl[HashCode, HashCode, Value]())
}

package common.cache.versioned

import common.cache.UniquelyHashable
import common.cache.sync.{GuavaBackedSynchronizedCache, KeyHashingSynchronizedCache}
import org.joda.time.Duration

trait VersionedKeyValueCache[Key, Version, Value] {

  def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value
}

object VersionedKeyValueCache {

  def apply[Key, Version, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new VersionedKeyValueCacheImpl[Key, Version, Value]()

  def hashingKeys[Key <: UniquelyHashable, Version <: UniquelyHashable, Value](): VersionedKeyValueCache[Key, Version, Value] =
    new HashingVersionedKeyValueCache(new VersionedKeyValueCacheImpl[String, String, Value]())
}

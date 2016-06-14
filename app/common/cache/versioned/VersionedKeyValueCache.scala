package common.cache.versioned

import java.util.concurrent.ConcurrentHashMap

final class VersionedKeyValueCache[Key, Version, Value]() {

  private val keyToEntries: ConcurrentHashMap[Key, CacheEntry[Value]] = new ConcurrentHashMap()

  def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value = {

  }

  case class CacheEntry[Value](version: Key, value: Value)
}

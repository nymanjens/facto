package common.cache.versioned

import java.util.concurrent.ConcurrentHashMap

private[versioned] final class VersionedKeyValueCacheImpl[Key, Version, Value]() extends VersionedKeyValueCache[Key, Version, Value] {

  private val keyToEntries: ConcurrentHashMap[Key, CacheEntry[Version, Value]] = new ConcurrentHashMap()

  def getOrCalculate(key: Key, version: Version, calculateValueFunc: () => Value): Value = {
    val entry = Option(keyToEntries.get(key))
    if (entry.isDefined && entry.get.version == version) {
      entry.get.value
    } else {
      val value = calculateValueFunc()
      keyToEntries.put(key, CacheEntry(version, value))
      value
    }
  }

  case class CacheEntry[Version, Value](version: Version, value: Value)
}

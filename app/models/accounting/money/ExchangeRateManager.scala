package models.accounting.money

import java.util.{NavigableMap, TreeMap}

import common.cache.CacheRegistry
import org.apache.http.annotation.GuardedBy
import org.joda.time.DateTime

import scala.collection.mutable

private[money] object ExchangeRateManager {
  CacheRegistry.registerCache(
    verifyConsistency = verifyConsistency,
    resetForTests = resetForTests)
  ExchangeRateMeasurements.addListener(measurementWasAdded)

  @GuardedBy("lock")
  private val measurementsCache: mutable.Map[Currency, NavigableMap[DateTime, Double]] = mutable.Map()
  private val lock = new Object

  def getRatioSecondToFirstCurrency(firstCurrency: Currency, secondCurrency: Currency, date: DateTime): Double = {
    (firstCurrency, secondCurrency) match {
      case (Currency.default, Currency.default) => 1.0
      case (foreignCurrency, Currency.default) =>
        ratioReferenceToForeignCurrency(foreignCurrency, date)
      case (Currency.default, foreignCurrency) =>
        1 / getRatioSecondToFirstCurrency(secondCurrency, firstCurrency, date)
      case _ =>
        throw new UnsupportedOperationException(s"Exchanging from non-reference to non-reference currency is not " +
          s"supported ($firstCurrency -> $secondCurrency)")
    }
  }

  private def ratioReferenceToForeignCurrency(currency: Currency, date: DateTime): Double = lock.synchronized {
    if (!(measurementsCache contains currency)) {
      measurementsCache.put(currency, fetchNavigableMap(currency))
    }

    val flooredEntry = Option(measurementsCache(currency).floorEntry(date))
    flooredEntry map (_.getValue) getOrElse 1.0
  }

  private def measurementWasAdded(m: ExchangeRateMeasurement): Unit = lock.synchronized {
    if (measurementsCache contains m.foreignCurrency) {
      measurementsCache(m.foreignCurrency).put(m.date, m.ratioReferenceToForeignCurrency)
    }
  }

  private def verifyConsistency(): Unit = lock.synchronized {
    for ((currency, map) <- measurementsCache) {
      val mapInDatabase = fetchNavigableMap(currency)
      require(
        mapInDatabase.size == map.size,
        s"Inconsistent cache for $currency: Sizes don't match: Database has ${mapInDatabase.size} entries, cache has ${map.size}.\n" +
          s"database measurements: $mapInDatabase\n" +
          s"cached map: $map")
      require(
        mapInDatabase == map,
        s"Inconsistent cache for $currency:.\n" +
          s"database measurements: $mapInDatabase\n" +
          s"cached map: $map")
    }
  }

  private def resetForTests(): Unit = lock.synchronized {
    measurementsCache.clear()
  }

  private def fetchNavigableMap(currency: Currency): NavigableMap[DateTime, Double] = {
    val map = new TreeMap[DateTime, Double]()
    for (measurement <- ExchangeRateMeasurements.fetchAll(currency)) {
      map.put(measurement.date, measurement.ratioReferenceToForeignCurrency)
    }
    map
  }
}

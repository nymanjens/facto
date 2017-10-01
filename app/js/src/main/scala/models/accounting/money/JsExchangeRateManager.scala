package models.accounting.money
import scala2js.Converters._
import scala.collection.immutable.{Seq, TreeMap}
import common.time.LocalDateTime
import models.access.RemoteDatabaseProxy
import models.manager.{EntityModification, EntityType}

import scala.collection.{SortedMap, mutable}
import scala2js.Keys

final class JsExchangeRateManager(implicit database: RemoteDatabaseProxy) extends ExchangeRateManager {
  database.registerListener(RemoteDatabaseProxyListener)

  /**
    * Cache for the `ExchangeRateMeasurement` entries in the database to improve performance of
    * `getRatioSecondToFirstCurrency()`.
    *
    * If this map has no entry for a currency, that means it has not been initialized yet.
    */
  private val measurementsCache: mutable.Map[Currency, SortedMap[LocalDateTime, Double]] = mutable.Map()

  // **************** Implementation of ExchangeRateManager trait ****************//
  override def getRatioSecondToFirstCurrency(firstCurrency: Currency,
                                             secondCurrency: Currency,
                                             date: LocalDateTime): Double = {
    (firstCurrency, secondCurrency) match {
      case (Currency.default, Currency.default) => 1.0
      case (foreignCurrency, Currency.default) =>
        ratioReferenceToForeignCurrency(foreignCurrency, date)
      case (Currency.default, foreignCurrency) =>
        1 / getRatioSecondToFirstCurrency(secondCurrency, firstCurrency, date)
      case _ =>
        throw new UnsupportedOperationException(
          s"Exchanging from non-reference to non-reference currency is not " +
            s"supported ($firstCurrency -> $secondCurrency)")
    }
  }

  // **************** Private helper methods ****************//
  private def ratioReferenceToForeignCurrency(currency: Currency, date: LocalDateTime): Double = {
    if (!(measurementsCache contains currency)) {
      measurementsCache.put(currency, fetchSortedMap(currency))
    }

    measurementsCache(currency).to(date).lastOption match {
      case Some((lastDate, lastRatio)) => lastRatio
      case None => 1.0
    }
  }

  private def fetchSortedMap(currency: Currency): SortedMap[LocalDateTime, Double] = {
    val mapBuilder = TreeMap.newBuilder[LocalDateTime, Double]
    for (measurement <- database
           .newQuery[ExchangeRateMeasurement]()
           .filterEqual(Keys.ExchangeRateMeasurement.foreignCurrencyCode, currency.code)
           .data()) {
      mapBuilder += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
    }
    mapBuilder.result()
  }

  // **************** Inner type definitions ****************//
  private object RemoteDatabaseProxyListener extends RemoteDatabaseProxy.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    override def addedRemotely(modifications: Seq[EntityModification]): Unit = {
      addedModifications(modifications)
    }

    private def addedModifications(modifications: Seq[EntityModification]): Unit = {
      for (modification <- modifications) {
        modification match {
          case EntityModification.Add(m: ExchangeRateMeasurement) =>
            // This happens infrequently so clearing the whole cache is not too expensive
            measurementsCache.clear()
          case EntityModification.Remove(_)
              if modification.entityType == EntityType.ExchangeRateMeasurementType =>
            // Measurements are normally not removed, but clearing the cache will make sure the cache gets updated later
            measurementsCache.clear()
          case _ => // do nothing
        }
      }
    }
  }
}

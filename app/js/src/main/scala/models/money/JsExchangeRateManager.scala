package models.money

import common.money.{Currency, ExchangeRateManager}
import common.time.LocalDateTime
import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.modification.EntityType
import models.modification.EntityModification

import scala.collection.immutable.{Seq, TreeMap}
import scala.collection.{SortedMap, mutable}
import scala2js.Converters._
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
    ???
    // val mapBuilder = TreeMap.newBuilder[LocalDateTime, Double]
    // for (measurement <-
    //        database
    //          .newQuery[ExchangeRateMeasurement]()
    //          .filter(Keys.ExchangeRateMeasurement.foreignCurrencyCode isEqualTo currency.code)
    //          .data()) {
    //   mapBuilder += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
    // }
    // mapBuilder.result()
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
        modification.entityType match {
          case EntityType.ExchangeRateMeasurementType =>
            modification match {
              case EntityModification.Add(_) =>
                // This happens infrequently so clearing the whole cache is not too expensive
                measurementsCache.clear()
              case EntityModification.Update(_) =>
                throw new UnsupportedOperationException("Immutable entity")
              case EntityModification.Remove(_) =>
                // Measurements are normally not removed, but clearing the cache will make sure the cache gets updated later
                measurementsCache.clear()
            }
          case _ => false // do nothing
        }
      }
    }
  }
}

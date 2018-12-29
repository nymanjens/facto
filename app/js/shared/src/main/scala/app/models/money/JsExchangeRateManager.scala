package app.models.money

import common.money.Currency
import common.money.ExchangeRateManager
import hydro.common.time.LocalDateTime
import app.models.access.AppJsEntityAccess
import app.models.modification.EntityModification
import app.models.modification.EntityType

import scala.collection.immutable.Seq
import scala.collection.SortedMap
import scala.collection.mutable
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._

final class JsExchangeRateManager(
    ratioReferenceToForeignCurrency: Map[Currency, SortedMap[LocalDateTime, Double]])(
    implicit entityAccess: AppJsEntityAccess)
    extends ExchangeRateManager {
  entityAccess.registerListener(JsEntityAccessListener)

  private val measurementsCache: mutable.Map[Currency, SortedMap[LocalDateTime, Double]] =
    mutable.Map(ratioReferenceToForeignCurrency.toVector: _*)

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
    measurementsCache.get(currency) match {
      case Some(dateToRatio) =>
        dateToRatio.to(date).lastOption match {
          case Some((lastDate, lastRatio)) => lastRatio
          case None                        => 1.0
        }
      case None => 1.0
    }
  }

  // **************** Inner type definitions ****************//
  private object JsEntityAccessListener extends AppJsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      for (modification <- modifications) {
        modification.entityType match {
          case EntityType.ExchangeRateMeasurementType =>
            modification match {
              case EntityModification.Add(e) =>
                // This happens infrequently
                val entity = e.asInstanceOf[ExchangeRateMeasurement]
                val currency = entity.foreignCurrency
                measurementsCache.put(
                  currency,
                  measurementsCache
                    .getOrElse(currency, SortedMap[LocalDateTime, Double]()) +
                    (entity.date -> entity.ratioReferenceToForeignCurrency))
              case EntityModification.Update(_) =>
                throw new UnsupportedOperationException("Immutable entity")
              case EntityModification.Remove(id) =>
                throw new UnsupportedOperationException("Measurements are normally not removed")
            }

          case _ => false // do nothing
        }
      }
    }
  }
}

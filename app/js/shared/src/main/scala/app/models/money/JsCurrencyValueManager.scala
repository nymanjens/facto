package app.models.money

import app.common.money.Currency
import app.common.money.CurrencyValueManager
import app.common.time.DatedMonth
import app.models.access.AppJsEntityAccess
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.models.modification.EntityModification
import hydro.common.time.LocalDateTime
import hydro.models.access.JsEntityAccess

import java.time.Duration
import scala.collection.immutable.Seq
import scala.collection.SortedMap
import scala.collection.mutable

final class JsCurrencyValueManager(
    initialRatioReferenceToForeignCurrency: Map[Currency, SortedMap[LocalDateTime, Double]]
)(implicit entityAccess: AppJsEntityAccess, clock: Clock)
    extends CurrencyValueManager {
  entityAccess.registerListener(JsEntityAccessListener)

  private val moneyValueIndexCurrency: Currency = Currency.General("<index>")

  private val measurementsCache: mutable.Map[Currency, SortedMap[LocalDateTime, Double]] =
    mutable.Map(initialRatioReferenceToForeignCurrency.toVector: _*)

  // **************** Implementation of CurrencyValueManager trait ****************//
  override def getRatioSecondToFirstCurrency(
      firstCurrency: Currency,
      secondCurrency: Currency,
      date: LocalDateTime,
  ): Double = {
    (firstCurrency, secondCurrency) match {
      case (Currency.default, Currency.default) => 1.0
      case (foreignCurrency, Currency.default) =>
        ratioReferenceToForeignCurrency(foreignCurrency, date)
      case (Currency.default, foreignCurrency) =>
        1 / getRatioSecondToFirstCurrency(secondCurrency, firstCurrency, date)
      case _ =>
        throw new UnsupportedOperationException(
          s"Exchanging from non-reference to non-reference currency is not " +
            s"supported ($firstCurrency -> $secondCurrency)"
        )
    }
  }

  override def getMoneyValueRatioHistoricalToToday(date: LocalDateTime): Double = {
    ratioReferenceToForeignCurrency(moneyValueIndexCurrency, clock.now) /
      ratioReferenceToForeignCurrency(moneyValueIndexCurrency, date)
  }

  override def getStartOfMeasurementsMonth(currency: Currency): DatedMonth = {
    measurementsCache.get(currency) match {
      case Some(dateToRatio) =>
        val dates = dateToRatio.keySet.toVector
        // Workaround: Filter first date if it is very far away from second measurement
        if (dates.length >= 2 && dates(1) - dates(0) > Duration.ofDays(70)) {
          DatedMonth.containing(dates(1))
        } else {
          DatedMonth.containing(dates(0))
        }
      case None => DatedMonth.current
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
  private object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      for (modification <- modifications) {
        modification.entityType match {
          case ExchangeRateMeasurement.Type =>
            modification match {
              case EntityModification.Add(e) =>
                // This happens infrequently
                val entity = e.asInstanceOf[ExchangeRateMeasurement]
                val currency = entity.foreignCurrency
                measurementsCache.put(
                  currency,
                  measurementsCache
                    .getOrElse(currency, SortedMap[LocalDateTime, Double]()) +
                    (entity.date -> entity.ratioReferenceToForeignCurrency),
                )
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

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

  private val measurementsCache: mutable.Map[Currency, SortedMap[LocalDateTime, Double]] =
    mutable.Map(initialRatioReferenceToForeignCurrency.toVector: _*)

  // **************** Implementation of CurrencyValueManager trait ****************//
  protected[app] override def ratioReferenceToForeignCurrencyDataPoints(
      currency: Currency
  ): SortedMap[LocalDateTime, Double] = {
    measurementsCache.getOrElse(currency, SortedMap())
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

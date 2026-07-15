package app.common.money

import hydro.common.time.JavaTimeImplicits._
import app.common.time.DatedMonth
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime

import java.time.Duration
import scala.collection.SortedMap

/** Converter for an amount of money from one currency into another. */
abstract class CurrencyValueManager(implicit clock: Clock) {

  private val moneyValueIndexCurrency: Currency = Currency.General("<index>")

  final def getRatioSecondToFirstCurrency(
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

  /** Returns the value of money at `date` divided by the value of the same amount today. */
  final def getMoneyValueRatioHistoricalToToday(date: LocalDateTime): Double = {
    ratioReferenceToForeignCurrency(moneyValueIndexCurrency, clock.now) /
      ratioReferenceToForeignCurrency(moneyValueIndexCurrency, date)
  }

  final def getStartOfMeasurementsMonth(currency: Currency): DatedMonth = {
    val dates = ratioReferenceToForeignCurrencyDataPoints(currency).keySet.toVector
    dates.length match {
      case 0 => DatedMonth.current
      case _ =>
        // Workaround: Filter first date if it is very far away from second measurement
        if (dates.length >= 2 && dates(1) - dates(0) > Duration.ofDays(70)) {
          DatedMonth.containing(dates(1))
        } else {
          DatedMonth.containing(dates(0))
        }
    }
  }

  // **************** Protected helper methods ****************//
  protected[app] def ratioReferenceToForeignCurrencyDataPoints(
      currency: Currency
  ): SortedMap[LocalDateTime, Double]

  // **************** Private helper methods ****************//
  private def ratioReferenceToForeignCurrency(currency: Currency, date: LocalDateTime): Double = {
    ratioReferenceToForeignCurrencyDataPoints(currency).to(date).lastOption match {
      case Some((lastDate, lastRatio)) => lastRatio
      case None                        => 1.0
    }
  }
}

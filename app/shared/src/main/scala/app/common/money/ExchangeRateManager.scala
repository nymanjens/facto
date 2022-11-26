package app.common.money

import hydro.common.time.LocalDateTime

/** Converter for an amount of money from one currency into another. */
trait ExchangeRateManager {

  def getRatioSecondToFirstCurrency(
      firstCurrency: Currency,
      secondCurrency: Currency,
      date: LocalDateTime,
  ): Double

  /** Returns the value of money at `date` divided by the value of the same amount today. */
  def getMoneyValueRatioHistoricalToToday(date: LocalDateTime): Double
}

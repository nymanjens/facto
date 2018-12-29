package app.common.money

import hydro.common.time.LocalDateTime

/** Converter for an amount of money from one currency into another. */
trait ExchangeRateManager {

  def getRatioSecondToFirstCurrency(firstCurrency: Currency,
                                    secondCurrency: Currency,
                                    date: LocalDateTime): Double
}

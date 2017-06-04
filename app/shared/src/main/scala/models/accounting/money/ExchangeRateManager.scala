package models.accounting.money

import common.time.LocalDateTime

/** Convertor for an amount of money from one currency into another. */
trait ExchangeRateManager {

  def getRatioSecondToFirstCurrency(firstCurrency: Currency,
                                    secondCurrency: Currency,
                                    date: LocalDateTime): Double
}

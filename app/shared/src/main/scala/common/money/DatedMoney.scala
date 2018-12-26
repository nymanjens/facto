package common.money

import common.GuavaReplacement.DoubleMath.roundToLong
import hydro.common.time.LocalDateTime

/**
  * Represents an amount of money that was spent or gained at a given date.
  *
  * The date allows the instance to be converted into other currences with the exchange rate of that day.
  */
case class DatedMoney(override val cents: Long, override val currency: Currency, date: LocalDateTime)
    extends MoneyWithGeneralCurrency {

  def exchangedForReferenceCurrency(implicit exchangeRateManager: ExchangeRateManager): ReferenceMoney =
    ReferenceMoney(exchangedForCurrency(Currency.default).cents)

  def exchangedForCurrency(otherCurrency: Currency)(
      implicit exchangeRateManager: ExchangeRateManager): DatedMoney = {
    val ratio = exchangeRateManager.getRatioSecondToFirstCurrency(currency, otherCurrency, date)
    val centsInOtherCurrency = roundToLong(ratio * cents)
    DatedMoney(centsInOtherCurrency, otherCurrency, date)
  }
}

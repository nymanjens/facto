package app.common.money

import hydro.common.GuavaReplacement.DoubleMath.roundToLong
import hydro.common.time.LocalDateTime

/**
 * Represents an amount of money that was spent or gained at a given date.
 *
 * The date allows the instance to be converted into other currences with the exchange rate of that day.
 */
case class DatedMoney(override val cents: Long, override val currency: Currency, date: LocalDateTime)
    extends MoneyWithGeneralCurrency {

  def exchangedForReferenceCurrency(
      correctForInflation: Boolean = false
  )(implicit currencyValueManager: CurrencyValueManager): ReferenceMoney = {
    val inflationCorrection = {
      if (correctForInflation) {
        currencyValueManager.getMoneyValueRatioHistoricalToToday(date)
      } else {
        1.0
      }
    }
    ReferenceMoney(exchangedForCurrency(Currency.default).cents) * inflationCorrection
  }

  def exchangedForCurrency(
      otherCurrency: Currency
  )(implicit currencyValueManager: CurrencyValueManager): DatedMoney = {
    val ratio = currencyValueManager.getRatioSecondToFirstCurrency(currency, otherCurrency, date)
    val centsInOtherCurrency = roundToLong(ratio * cents)
    DatedMoney(centsInOtherCurrency, otherCurrency, date)
  }
}

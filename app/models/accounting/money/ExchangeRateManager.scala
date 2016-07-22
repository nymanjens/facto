package models.accounting.money

import org.joda.time.DateTime

private[money] object ExchangeRateManager {

  private val ratioReferenceToForeignCurrency: Map[Currency, Double] = Map(Currency.Gbp -> 1.3)

  def getRatioSecondToFirstCurrency(firstCurrency: Currency, secondCurrency: Currency, date: DateTime): Double = {
    (firstCurrency, secondCurrency) match {
      case (Currency.default, Currency.default) => 1.0
      case (foreignCurrency, Currency.default) =>
        ratioReferenceToForeignCurrency(foreignCurrency)
      case (Currency.default, foreignCurrency) =>
        1 / getRatioSecondToFirstCurrency(secondCurrency, foreignCurrency, date)
      case _ =>
        throw new UnsupportedOperationException(s"Exchanging from non-reference to non-reference currency is not supported")
    }
  }
}

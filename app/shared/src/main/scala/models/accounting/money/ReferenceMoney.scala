package models.accounting.money

import models.accounting.money.CentOperations.CentOperationsNumeric

/**
  * Represents an amount of money in the (default) reference currency.
  *
  * Note that this can't be converted into other currencies since we don't know what date we should assume for the
  * exchange rate.
  */
case class ReferenceMoney(override val cents: Long) extends Money with CentOperations[ReferenceMoney] {

  override def currency = Currency.default

  override protected def withCents(newCents: Long): ReferenceMoney = copy(cents = newCents)

  override protected def validateCentOperation(that: ReferenceMoney): Unit = {}

  override def toHtmlWithCurrency(implicit exchangeRateManager: ExchangeRateManager): String =
    Money.centsToHtmlWithCurrency(cents, Currency.default)
}

object ReferenceMoney {
  implicit object MoneyNumeric extends CentOperationsNumeric[ReferenceMoney] {
    override def fromInt(x: Int): ReferenceMoney = ReferenceMoney(0)
  }
}

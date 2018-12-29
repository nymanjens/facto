package app.common.money

import app.common.money.CentOperations.CentOperationsNumeric

/**
  * Base class for an amount of money with an arbitrary currency.
  */
trait MoneyWithGeneralCurrency extends Money with CentOperations[MoneyWithGeneralCurrency] {

  override protected final def withCents(newCents: Long): MoneyWithGeneralCurrency =
    MoneyWithGeneralCurrencyImpl(newCents, currency)

  override protected final def validateCentOperation(that: MoneyWithGeneralCurrency): Unit = {
    require(this.currency == that.currency, s"The currencies of ${this} and ${that} differ")
  }
}

object MoneyWithGeneralCurrency {

  def apply(cents: Long, currency: Currency): MoneyWithGeneralCurrency = {
    MoneyWithGeneralCurrencyImpl(cents, currency)
  }

  def numeric(currency: Currency): Numeric[MoneyWithGeneralCurrency] =
    new CentOperationsNumeric[MoneyWithGeneralCurrency] {
      override def fromInt(x: Int): MoneyWithGeneralCurrency = MoneyWithGeneralCurrencyImpl(0, currency)
    }
}

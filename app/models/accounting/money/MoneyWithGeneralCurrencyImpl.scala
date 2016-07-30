package models.accounting.money

import play.twirl.api.Html

private[money] case class MoneyWithGeneralCurrencyImpl(override val cents: Long, currency: Currency) extends MoneyWithGeneralCurrency {

  override def toHtmlWithCurrency: Html = {
    Money.centsToHtmlWithCurrency(cents, currency)
  }
}

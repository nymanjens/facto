package models.accounting.money

import models.accounting.money.CentOperations.CentOperationsNumeric
import org.joda.time.DateTime
import play.twirl.api.Html

case class ReferenceMoney(override val cents: Long) extends Money with CentOperations[ReferenceMoney] {

  override def currency = Currency.default

  override protected def withCents(newCents: Long): ReferenceMoney = copy(cents = newCents)

  override protected def validateCentOperation(that: ReferenceMoney): Unit = {}

  override def toHtmlWithCurrency: Html = Money.centsToHtmlWithCurrency(cents, Currency.default)

  def exchangedForCurrency(currency: Currency, date: DateTime): DatedMoney = {
    // TODO: Apply exchange rate
    DatedMoney(cents, currency, date)
  }
}

object ReferenceMoney {
  implicit object MoneyNumeric extends CentOperationsNumeric[ReferenceMoney] {
    override def fromInt(x: Int): ReferenceMoney = ReferenceMoney(0)
  }
}

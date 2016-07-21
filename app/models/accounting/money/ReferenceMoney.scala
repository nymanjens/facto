package models.accounting.money

import models.accounting.money.CentOperations.CentOperationsNumeric
import org.joda.time.DateTime
import play.twirl.api.Html

case class ReferenceMoney(override val cents: Long) extends CentOperations[ReferenceMoney] {

  override protected def withCents(newCents: Long): ReferenceMoney = ReferenceMoney(newCents)

  override protected def validateCentOperation(that: ReferenceMoney): Unit = {}

  def formatFloat: String = Money.centsToFloatString(cents)

  def toHtmlWithCurrency: Html = Money.centsToHtmlWithCurrency(cents, Currency.default)

  def toMoney(date: DateTime): Money = Money(cents, Currency.default)

  def exchangedForCurrency(currency: Currency, date: DateTime): Money = {
    // TODO: Apply exchange rate
    Money(cents, currency)
  }
}

object ReferenceMoney {
  implicit object MoneyNumeric extends CentOperationsNumeric[ReferenceMoney] {
    override def fromInt(x: Int): ReferenceMoney = ReferenceMoney(0)
  }
}

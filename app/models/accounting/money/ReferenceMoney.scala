package models.accounting.money

import models.accounting.money.CentOperations.CentOperationsNumeric
import org.joda.time.DateTime
import play.twirl.api.Html

case class ReferenceMoney(override val cents: Long) extends CentOperations[ReferenceMoney] {

  override protected def withCents(newCents: Long): ReferenceMoney = ReferenceMoney(newCents)

  override protected def validateCentOperation(that: ReferenceMoney): Unit = {}

  def formatFloat: String = Money.centsToFloatString(cents)

  def toHtmlWithCurrency: Html = toMoney.toHtmlWithCurrency

  def toMoney: Money = Money(cents, CurrencyUnit.default)

  def exchangedForCurrency(currencyUnit: CurrencyUnit, date: DateTime): Money = {
    // TODO: Apply exchange rate
    Money(cents, currencyUnit)
  }
}

object ReferenceMoney {
  implicit object MoneyNumeric extends CentOperationsNumeric[ReferenceMoney] {
    override def fromInt(x: Int): ReferenceMoney = ReferenceMoney(0)
  }
}

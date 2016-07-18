package models.accounting.money

import models.accounting.money.CentOperations.CentOperationsNumeric

case class ReferenceMoney(override val cents: Long) extends CentOperations[ReferenceMoney] {

  override protected def withCents(newCents: Long): ReferenceMoney = ReferenceMoney(newCents)

  override protected def validateCentOperation(that: ReferenceMoney): Unit = {}

  def formatFloat: String = Money.centsToFloatString(cents)

  def toMoney: Money = Money(cents, CurrencyUnit.default)
}

object ReferenceMoney {
  implicit object MoneyNumeric extends CentOperationsNumeric[ReferenceMoney] {
    override def fromInt(x: Int): ReferenceMoney = ReferenceMoney(0)
  }
}

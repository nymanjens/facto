package models.accounting.money

import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import com.google.common.collect.Iterables
import models.accounting.config.Config
import models.accounting.money.CentOperations.CentOperationsNumeric
import play.twirl.api.Html

import scala.collection.JavaConverters._

case class Money(override val cents: Long, currency: CurrencyUnit = CurrencyUnit.default) extends CentOperations[Money] {

  def formatFloat: String = Money.centsToFloatString(cents)

  override def toString = s"${currency.threeLetterSymbol} $formatFloat"

  override def doCentOperation[T](operation: (Long, Long) => T)(that: Money): T = {
    if (this.cents != 0 && that.cents != 0) {
      require(this.currency == that.currency, s"The currencies of ${this} and ${that} differ")
    }
    operation(this.cents, that.cents)
  }
  override def doCentOperationToSelfType(operation: (Long, Long) => Long)(that: Money): Money = {
    Money(doCentOperation(operation)(that), this.currency)
  }

  override def withCents(newCents: Long): Money = copy(cents = newCents)
}

object Money {

  val zero: Money = Money(0)

  def centsToFloatString(cents: Long): String = {
    val sign = if (cents < 0) "-" else ""
    val integerPart = NumberFormat.getNumberInstance(Locale.US).format(abs(cents) / 100)
    val centsPart = abs(cents % 100)
    "%s%s.%02d".format(sign, integerPart, centsPart)
  }

  def floatToCents(float: Double): Long =
    (float.toDouble * 100).round

  implicit object MoneyNumeric extends CentOperationsNumeric[Money] {
    override def fromInt(x: Int): Money = Money.zero
  }
}

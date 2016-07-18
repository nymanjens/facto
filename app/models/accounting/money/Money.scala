package models.accounting.money

import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import com.google.common.collect.Iterables
import models.accounting.config.Config
import models.accounting.money.CentOperations.CentOperationsNumeric
import play.twirl.api.Html

import scala.collection.JavaConverters._

// TODO: Remove default currency
case class Money(override val cents: Long, currency: CurrencyUnit = CurrencyUnit.default) extends CentOperations[Money] {

  override protected def withCents(newCents: Long): Money = copy(cents = newCents)

  override protected def validateCentOperation(that: Money): Unit = {
    require(this.currency == that.currency, s"The currencies of ${this} and ${that} differ")
  }

  override def toString = s"${currency.threeLetterSymbol} $formatFloat"

  def formatFloat: String = Money.centsToFloatString(cents)

  def toReferenceCurrency: ReferenceMoney = {
    // TODO: Apply exchange rate
    ReferenceMoney(cents)
  }
}

object Money {

  def centsToFloatString(cents: Long): String = {
    val sign = if (cents < 0) "-" else ""
    val integerPart = NumberFormat.getNumberInstance(Locale.US).format(abs(cents) / 100)
    val centsPart = abs(cents % 100)
    "%s%s.%02d".format(sign, integerPart, centsPart)
  }

  def floatToCents(float: Double): Long =
    (float.toDouble * 100).round

  // TODO: Remove this
  implicit object MoneyNumeric extends CentOperationsNumeric[Money] {
    override def fromInt(x: Int): Money = Money(0)
  }
}

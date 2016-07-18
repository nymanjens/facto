package models.accounting.money

import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import com.google.common.collect.Iterables
import models.accounting.config.Config
import play.twirl.api.Html

import scala.collection.JavaConverters._

case class Money(cents: Long, currency: CurrencyUnit = CurrencyUnit.default) {

  def negated: Money = withCents(-cents)

  def +(that: Money): Money = doCentOperationToMoney(_ + _)(that)
  def -(that: Money): Money = doCentOperationToMoney(_ - _)(that)
  def *(number: Long): Money = withCents(cents * number)
  def /(number: Long): Money = withCents(round(cents * 1.0 / number))
  def ==(that: Money): Boolean = doCentOperation(_ == _)(that)
  def >(that: Money): Boolean = doCentOperation(_ > _)(that)
  def <(that: Money): Boolean = doCentOperation(_ < _)(that)
  def >=(that: Money): Boolean = doCentOperation(_ >= _)(that)
  def <=(that: Money): Boolean = doCentOperation(_ <= _)(that)

  def formatFloat: String = Money.centsToFloatString(cents)

  override def toString = s"${currency.threeLetterSymbol} $formatFloat"

  private def doCentOperation[T](operation: (Long, Long) => T)(that: Money): T = {
    require(this.currency == that.currency, s"${this.currency} is different from ${that.currency}")
    operation(this.cents, that.cents)
  }
  private def doCentOperationToMoney(operation: (Long, Long) => Long)(that: Money): Money =
    withCents(doCentOperation(operation)(that))

  private def withCents(newCents: Long): Money = copy(cents = newCents)
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

  implicit object MoneyNumeric extends Numeric[Money] {
    override def negate(x: Money): Money = x.negated
    override def plus(x: Money, y: Money): Money = x + y
    override def minus(x: Money, y: Money): Money = x - y
    override def times(x: Money, y: Money): Money = throw new UnsupportedOperationException("Multiplication of Money doesn't make sense.")

    override def toDouble(x: Money): Double = x.cents.toDouble
    override def toFloat(x: Money): Float = x.cents.toFloat
    override def toInt(x: Money): Int = x.cents.toInt
    override def toLong(x: Money): Long = x.cents

    override def fromInt(x: Int): Money = Money(x)
    override def compare(x: Money, y: Money): Int = (x.cents - y.cents).signum
  }
}

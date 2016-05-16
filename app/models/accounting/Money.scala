package models.accounting

import scala.collection.JavaConverters._

import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import play.twirl.api.Html

import com.google.common.collect.Iterables

import models.accounting.config.Config
import models.accounting.Money.CurrencyUnit

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

  def formatFloat: String = {
    val sign = if (cents < 0) "-" else ""
    val integerPart = NumberFormat.getNumberInstance(Locale.US).format(abs(cents) / 100)
    val centsPart = abs(cents % 100)
    "%s%s.%02d".format(sign, integerPart, centsPart)
  }

  override def toString = s"${currency.threeLetterSymbol} $formatFloat"

  private def doCentOperation[T](operation: (Long, Long) => T)(that: Money): T = {
    require(this.currency == that.currency)
    operation(this.cents, that.cents)
  }
  private def doCentOperationToMoney(operation: (Long, Long) => Long)(that: Money): Money =
    withCents(doCentOperation(operation)(that))

  private def withCents(newCents: Long): Money = copy(cents = newCents)
}

object Money {

  def fromFloat(float: Double, currency: CurrencyUnit = CurrencyUnit.default): Money =
    Money((float.toDouble * 100).round, currency)

  sealed abstract class CurrencyUnit(val threeLetterSymbol: String, val htmlSymbol: Html, val iconClass: Option[String] = None) {
    override def toString = threeLetterSymbol
  }

  object CurrencyUnit {
    def of(threeLetterSymbol: String): CurrencyUnit = {
      val candidates = all.filter(_.threeLetterSymbol.toLowerCase == threeLetterSymbol.toLowerCase)
      Iterables.getOnlyElement(candidates.asJava)
    }

    lazy val default: CurrencyUnit = CurrencyUnit.of(Config.constants.defaultCurrencySymbol)

    private def all: Set[CurrencyUnit] = Set(Eur, Gbp, Usd)
    object Eur extends CurrencyUnit("EUR", Html("&euro;"), Some("fa fa-eur"))
    object Gbp extends CurrencyUnit("GBP", Html("&pound;"), Some("fa fa-gbp"))
    object Usd extends CurrencyUnit("USD", Html("$"), Some("fa fa-usd"))
  }

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

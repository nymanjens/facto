package models.accounting.money

import scala.collection.immutable.Seq
import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import com.google.common.collect.Iterables
import models.accounting.config.Config
import models.accounting.money.CentOperations.CentOperationsNumeric
import org.joda.time.DateTime
import play.twirl.api.Html

import scala.collection.JavaConverters._

case class Money(override val cents: Long, currency: CurrencyUnit) extends CentOperations[Money] {

  override protected def withCents(newCents: Long): Money = copy(cents = newCents)

  override protected def validateCentOperation(that: Money): Unit = {
    require(this.currency == that.currency, s"The currencies of ${this} and ${that} differ")
  }

  override def toString = s"${currency.threeLetterSymbol} $formatFloat"

  def formatFloat: String = Money.centsToFloatString(cents)

  def toHtmlWithCurrency: Html = {
    import Money.SummableHtml
    val baseHtml = currency.htmlSymbol ++ s" ${formatFloat}"
    if (currency == CurrencyUnit.default) {
      baseHtml
    } else {
      val defaultCurrencyHtml = exchangedForReferenceCurrency.toHtmlWithCurrency
      baseHtml ++ """ <span class="reference-currency">""" ++ defaultCurrencyHtml ++ "</span>"
    }
  }

  def exchangedForReferenceCurrency: ReferenceMoney = {
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

  def moneyNumeric(currency: CurrencyUnit): Numeric[Money] = new CentOperationsNumeric[Money] {
    override def fromInt(x: Int): Money = Money(0, currency)
  }

  private implicit class SummableHtml(html: Html) {
    def ++(string: String): Html = {
      this ++ Html(string)
    }
    def ++(otherHtml: Html): Html = {
      new Html(Seq(html, otherHtml))
    }
  }
}

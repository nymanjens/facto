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

/**
  * Base class for any type that represents an amount of money.
  *
  * Although this has a `currency` method, implementations are allowed to pick a single fixed currency.
  */
trait Money {

  def cents: Long
  def currency: Currency
  def toHtmlWithCurrency: Html

  final def formatFloat: String = Money.centsToFloatString(cents)

  final def withDate(date: DateTime): DatedMoney = {
    DatedMoney(cents, currency, date)
  }

  override def toString = s"${currency.code} $formatFloat"
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

  private[money] def centsToHtmlWithCurrency(cents: Long, currency: Currency): Html = {
    currency.htmlSymbol ++ s" ${centsToFloatString(cents)}"
  }

  private[money] implicit class SummableHtml(html: Html) {
    def ++(string: String): Html = {
      this ++ Html(string)
    }
    def ++(otherHtml: Html): Html = {
      new Html(Seq(html, otherHtml))
    }
  }
}

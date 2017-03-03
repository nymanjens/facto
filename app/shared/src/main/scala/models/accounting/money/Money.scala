package models.accounting.money

import scala.collection.immutable.Seq
import java.lang.Math.{abs, round}
import java.util.Locale

import models.accounting.config.Config
import models.accounting.money.CentOperations.CentOperationsNumeric
import common.time.LocalDateTime
import common.GuavaReplacement.DoubleMath.roundToLong

import scala.collection.JavaConverters._

/**
  * Base class for any type that represents an amount of money.
  *
  * Although this has a `currency` method, implementations are allowed to pick a single fixed currency.
  */
trait Money {

  def cents: Long
  def currency: Currency
  def toHtmlWithCurrency(implicit exchangeRateManager: ExchangeRateManager): String

  final def formatFloat: String = Money.centsToFloatString(cents)

  final def withDate(date: LocalDateTime): DatedMoney = {
    DatedMoney(cents, currency, date)
  }

  override def toString = s"${currency.code} $formatFloat"
}

object Money {

  def centsToFloatString(cents: Long): String = {
    val sign = if (cents < 0) "-" else ""
    val integerPart = roundToLong(abs(cents) / 100)
    val centsPart = abs(cents % 100)
    "%s%s.%02d".format(sign, integerPart, centsPart)
  }

  def floatToCents(float: Double): Long =
    (float.toDouble * 100).round

  private[money] def centsToHtmlWithCurrency(cents: Long, currency: Currency): String = {
    currency.symbol + s" ${centsToFloatString(cents)}"
  }
}

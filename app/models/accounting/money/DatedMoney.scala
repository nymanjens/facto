package models.accounting.money

import java.lang.Math.round

import scala.collection.immutable.Seq
import java.lang.Math.{abs, round}
import java.text.NumberFormat
import java.util.Locale

import com.google.common.collect.Iterables
import com.google.common.math.DoubleMath.roundToLong
import models.accounting.config.Config
import models.accounting.money.CentOperations.CentOperationsNumeric
import org.joda.time.DateTime
import play.twirl.api.Html

import scala.collection.JavaConverters._
import java.math.RoundingMode.HALF_EVEN

case class DatedMoney(override val cents: Long, override val currency: Currency, date: DateTime) extends MoneyWithGeneralCurrency {

  override def toHtmlWithCurrency: Html = {
    import Money.SummableHtml
    val baseHtml = Money.centsToHtmlWithCurrency(cents, currency)
    if (currency == Currency.default) {
      baseHtml
    } else {
      val defaultCurrencyHtml = exchangedForReferenceCurrency.toHtmlWithCurrency
      baseHtml ++ """ <span class="reference-currency">""" ++ defaultCurrencyHtml ++ "</span>"
    }
  }

  def exchangedForReferenceCurrency: ReferenceMoney =
    ReferenceMoney(exchangedForCurrency(Currency.default).cents)

  def exchangedForCurrency(otherCurrency: Currency): DatedMoney = {
    val ratio = ExchangeRateManager.getRatioSecondToFirstCurrency(currency, otherCurrency, date)
    val centsInOtherCurrency = roundToLong(ratio * cents, HALF_EVEN)
    DatedMoney(centsInOtherCurrency, currency, date)
  }
}

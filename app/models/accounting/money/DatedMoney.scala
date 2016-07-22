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

  def exchangedForCurrency(currency: Currency): DatedMoney = {
    // TODO: Apply exchange rate
    DatedMoney(cents, currency, date)
  }
}

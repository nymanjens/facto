package models.accounting.money

import com.google.common.collect.Iterables
import models.accounting.config.Config
import play.twirl.api.Html

import scala.collection.JavaConverters._

sealed abstract class CurrencyUnit(val threeLetterSymbol: String, val htmlSymbol: Html, val iconClass: Option[String] = None) {
  override def toString = threeLetterSymbol
}

object CurrencyUnit {
  def of(threeLetterSymbol: String): CurrencyUnit = {
    val candidates = all.filter(_.threeLetterSymbol.toLowerCase == threeLetterSymbol.toLowerCase)
    Iterables.getOnlyElement(candidates.asJava)
  }

  lazy val default: CurrencyUnit = CurrencyUnit.of(Config.constants.defaultCurrency)

  private def all: Set[CurrencyUnit] = Set(Eur, Gbp, Usd)
  object Eur extends CurrencyUnit("EUR", Html("&euro;"), Some("fa fa-eur"))
  object Gbp extends CurrencyUnit("GBP", Html("&pound;"), Some("fa fa-gbp"))
  object Usd extends CurrencyUnit("USD", Html("$"), Some("fa fa-usd"))
}

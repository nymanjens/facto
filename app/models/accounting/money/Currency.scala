package models.accounting.money

import com.google.common.collect.Iterables
import models.accounting.config.Config
import play.twirl.api.Html

import scala.collection.JavaConverters._

/**
  * @param threeLetterSymbol The three letter symbol.
  */
sealed abstract class Currency(val threeLetterSymbol: String, val htmlSymbol: Html, val iconClass: Option[String] = None) {
  override def toString = threeLetterSymbol
}

object Currency {
  def of(threeLetterSymbol: String): Currency = {
    val candidates = all.filter(_.threeLetterSymbol.toLowerCase == threeLetterSymbol.toLowerCase)
    Iterables.getOnlyElement(candidates.asJava)
  }

  lazy val default: Currency = Currency.of(Config.constants.defaultCurrency)

  private def all: Set[Currency] = Set(Eur, Gbp, Usd)
  object Eur extends Currency("EUR", Html("&euro;"), Some("fa fa-eur"))
  object Gbp extends Currency("GBP", Html("&pound;"), Some("fa fa-gbp"))
  object Usd extends Currency("USD", Html("$"), Some("fa fa-usd"))
}

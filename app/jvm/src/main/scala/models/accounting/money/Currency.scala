package models.accounting.money

import com.google.common.collect.Iterables
import models.accounting.config.Config
import play.twirl.api.Html

import scala.collection.JavaConverters._

/**
  * @param code The three letter symbol.
  */
sealed abstract class Currency(val code: String, val htmlSymbol: Html, val iconClassOption: Option[String] = None) {
  def iconClass: String = iconClassOption.getOrElse("fa fa-money")
  def isForeign: Boolean = this != Currency.default
  override def toString = code
}

object Currency {
  def of(code: String): Currency = {
    val candidates = all.filter(_.code.toLowerCase == code.toLowerCase)
    Iterables.getOnlyElement(candidates.asJava)
  }

  // TOOD: Make this configurable
  val default: Currency = Eur

  private def all: Set[Currency] = Set(Eur, Gbp, Usd)
  object Eur extends Currency("EUR", Html("&euro;"), Some("fa fa-eur"))
  object Gbp extends Currency("GBP", Html("&pound;"), Some("fa fa-gbp"))
  object Usd extends Currency("USD", Html("$"), Some("fa fa-usd"))
}

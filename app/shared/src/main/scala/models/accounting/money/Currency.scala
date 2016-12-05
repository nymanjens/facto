package models.accounting.money

import common.GuavaReplacement.Iterables.getOnlyElement

/**
  * @param code The three letter symbol.
  */
sealed abstract class Currency(val code: String, val htmlSymbol: String, val iconClassOption: Option[String] = None) {
  def iconClass: String = iconClassOption.getOrElse("fa fa-money")
  def isForeign: Boolean = this != Currency.default
  override def toString = code
}

object Currency {
  def of(code: String): Currency = {
    val candidates = all.filter(_.code.toLowerCase == code.toLowerCase)
    getOnlyElement(candidates)
  }

  // TOOD: Make this configurable
  val default: Currency = Eur

  private def all: Set[Currency] = Set(Eur, Gbp, Usd)
  object Eur extends Currency("EUR", "&euro;", Some("fa fa-eur"))
  object Gbp extends Currency("GBP", "&pound;", Some("fa fa-gbp"))
  object Usd extends Currency("USD", "$", Some("fa fa-usd"))
}

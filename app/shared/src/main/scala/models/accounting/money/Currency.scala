package models.accounting.money

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
    val candidates = allCustom.filter(_.code.toLowerCase == code.toLowerCase)

    candidates.toSeq match {
      case Seq(currency) => currency
      case Seq() => General(code)
    }
  }

  // TODO: Make this configurable
  val default: Currency = Eur

  private def allCustom: Set[Currency] = Set(Eur, Gbp, Usd)
  object Eur extends Currency("EUR", "€", Some("fa fa-eur"))
  object Gbp extends Currency("GBP", "£", Some("fa fa-gbp"))
  object Usd extends Currency("USD", "$", Some("fa fa-usd"))
  case class General(symbol: String) extends Currency(symbol, symbol, None)
}

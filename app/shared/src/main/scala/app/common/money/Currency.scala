package app.common.money

/**
  * @param code The three letter symbol.
  */
sealed abstract class Currency(val code: String,
                               val symbol: String,
                               val iconClassOption: Option[String] = None) {
  def iconClass: String = iconClassOption.getOrElse("fa fa-money")
  def isForeign: Boolean = this != Currency.default
  override def toString = code
}

object Currency {
  def of(code: String): Currency = {
    val candidates = allStandardCurrencies.filter(_.code.toLowerCase == code.toLowerCase)

    candidates.toSeq match {
      case Seq(currency) => currency
      case Seq()         => General(code)
    }
  }

  // TODO: Make this configurable
  val default: Currency = Eur

  private def allStandardCurrencies: Set[Currency] = Set(Eur, Gbp, Usd)
  object Eur extends Currency("EUR", "€", Some("fa fa-eur"))
  object Gbp extends Currency("GBP", "£", Some("fa fa-gbp"))
  object Usd extends Currency("USD", "$", Some("fa fa-usd"))
  case class General(codeAndSymbol: String) extends Currency(codeAndSymbol, codeAndSymbol, None)
}

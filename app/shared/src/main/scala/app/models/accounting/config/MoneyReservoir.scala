package app.models.accounting.config

import app.common.Require.requireNonNull
import app.common.money.Currency

case class MoneyReservoir(code: String,
                          name: String,
                          shorterName: String,
                          owner: Account,
                          hidden: Boolean,
                          currencyCode: Option[String] = None) {
  requireNonNull(code, name, shorterName, owner, hidden, currencyCode)

  override def toString = s"MoneyReservoir($code)"

  lazy val currency: Currency = currencyCode match {
    case Some(code) => Currency.of(code)
    case None       => Currency.default
  }

  def isNullReservoir: Boolean = this.code.isEmpty
}

object MoneyReservoir {
  val NullMoneyReservoir: MoneyReservoir = MoneyReservoir(
    code = "",
    name = "N/A",
    shorterName = "N/A",
    owner = Account(
      code = "UNKNOWN",
      longName = "UNKNOWN",
      shorterName = "UNKNOWN",
      veryShortName = "UNKNOWN",
      defaultElectronicReservoirCode = ""),
    hidden = true
  )
}

package models.accounting.config

import common.Require.requireNonNullFields
import models.accounting.Money.CurrencyUnit

case class MoneyReservoir(code: String, name: String, shorterName: String, owner: Account, hidden: Boolean) {
  requireNonNullFields(this)

  override def toString = s"MoneyReservoir($code)"

  def currency: CurrencyUnit = CurrencyUnit.default
}

object MoneyReservoir {
  val NullMoneyReservoir: MoneyReservoir = MoneyReservoir(
    code = "",
    name = "N/A",
    shorterName = "N/A",
    owner = Account("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN"),
    hidden = true)
}


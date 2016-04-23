package models.accounting.config

import common.Require.requireNonNullFields
import models.accounting.Money.CurrencyUnit

case class MoneyReservoir(code: String, name: String, owner: Account) {
  requireNonNullFields(this)
  
  override def toString = s"MoneyReservoir($code)"

  def currency: CurrencyUnit = CurrencyUnit.default
}

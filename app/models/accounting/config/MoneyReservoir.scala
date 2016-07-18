package models.accounting.config

import common.Require.requireNonNullFields
import models.accounting.Money.CurrencyUnit

case class MoneyReservoir(code: String,
                          name: String,
                          shorterName: String,
                          owner: Account,
                          hidden: Boolean,
                          private val currencyCode: Option[String] = None) {
  requireNonNullFields(this, lazyFieldNames = Set("currency"))

  override def toString = s"MoneyReservoir($code)"

  lazy val currency: CurrencyUnit = currencyCode match {
    case Some(code) => CurrencyUnit.of(code)
    case None => CurrencyUnit.default
  }
}

object MoneyReservoir {
  val NullMoneyReservoir: MoneyReservoir = MoneyReservoir(
    code = "",
    name = "N/A",
    shorterName = "N/A",
    owner = Account(code = "UNKNOWN", longName = "UNKNOWN", shorterName = "UNKNOWN", veryShortName = "UNKNOWN", defaultElectronicReservoirCode = ""),
    hidden = true)
}


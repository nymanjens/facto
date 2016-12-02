package models.accounting.config

import common.Require.requireNonNullFields
import models.accounting.money.Currency
import models.accounting.money.Money

case class MoneyReservoir(code: String,
                          name: String,
                          shorterName: String,
                          owner: Account,
                          hidden: Boolean,
                          private val currencyCode: Option[String] = None) {
  requireNonNullFields(this, lazyFieldNames = Set("currency"))

  override def toString = s"MoneyReservoir($code)"

  lazy val currency: Currency = currencyCode match {
    case Some(code) => Currency.of(code)
    case None => Currency.default
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


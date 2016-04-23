package models.accounting.config

import common.Require.requireNonNullFields

case class Constants(commonAccount: Account,
                     accountingCategory: Category,
                     endowmentCategory: Category,
                     defaultElectronicMoneyReservoirByAccount: Map[Account, MoneyReservoir],
                     liquidationDescription: String,
                     defaultCurrencySymbol: String) {
  requireNonNullFields(this)
}
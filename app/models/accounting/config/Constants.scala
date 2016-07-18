package models.accounting.config

import common.Require.requireNonNullFields

case class Constants(commonAccount: Account,
                     accountingCategory: Category,
                     endowmentCategory: Category,
                     liquidationDescription: String,
                     defaultCurrency: String) {
  requireNonNullFields(this)
}

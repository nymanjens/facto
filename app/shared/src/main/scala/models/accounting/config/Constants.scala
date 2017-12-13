package models.accounting.config

import common.Require.requireNonNull

case class Constants(commonAccount: Account,
                     accountingCategory: Category,
                     endowmentCategory: Category,
                     liquidationDescription: String,
                     zoneId: String) {
  requireNonNull(commonAccount, accountingCategory, endowmentCategory, liquidationDescription, zoneId)
}

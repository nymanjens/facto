package app.models.accounting.config

import app.common.Require.requireNonNull

case class Constants(commonAccount: Account,
                     accountingCategory: Category,
                     endowmentCategory: Category,
                     liquidationDescription: String,
                     zoneId: String) {
  requireNonNull(commonAccount, accountingCategory, endowmentCategory, liquidationDescription, zoneId)
}

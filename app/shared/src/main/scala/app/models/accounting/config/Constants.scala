package app.models.accounting.config

import hydro.common.Require.requireNonNull

case class Constants(
    commonAccount: Account,
    accountingCategory: Category,
    endowmentCategory: Category,
    liquidationDescription: String,
    zoneId: String,
    // If true, then a button will be shown to enable/disable inflation corrections. These are made using the price
    // index stored in the exchange rate of a special "<index>" currency.
    supportInflationCorrections: Boolean,
) {
  requireNonNull(commonAccount, accountingCategory, endowmentCategory, liquidationDescription, zoneId)
}

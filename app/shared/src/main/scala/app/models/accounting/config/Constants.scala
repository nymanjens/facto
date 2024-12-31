package app.models.accounting.config

import hydro.common.Require.requireNonNull

import java.time.Month

case class Constants(
    commonAccount: Account,
    accountingCategory: Category,
    endowmentCategory: Category,
    liquidationDescription: String,
    zoneId: String,
    // If true, then a button will be shown to enable/disable inflation corrections. These are made using the price
    // index stored in the exchange rate of a special "<index>" currency.
    supportInflationCorrections: Boolean,
    // Whenever facto shows a year, this will be the first month. If this is January, calendar years are used.
    firstMonthOfYear: Month,
) {
  requireNonNull(commonAccount, accountingCategory, endowmentCategory, liquidationDescription, zoneId, firstMonthOfYear)
}

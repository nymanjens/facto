package app.common.accounting

import app.common.money.CurrencyValueManager
import app.models.access.AppEntityAccess
import app.models.accounting.config.Config

final class Module(implicit
                   accountingConfig: Config,
                   currencyValueManager: CurrencyValueManager,
                   entityAccess: AppEntityAccess,
) {
  implicit lazy val templateMatcher: TemplateMatcher = new TemplateMatcher()
}

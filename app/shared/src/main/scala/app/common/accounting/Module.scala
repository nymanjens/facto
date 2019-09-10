package app.common.accounting

import app.common.money.ExchangeRateManager
import app.models.access.AppEntityAccess
import app.models.accounting.config.Config

final class Module(
    implicit accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    entityAccess: AppEntityAccess,
) {
  implicit lazy val templateMatcher: TemplateMatcher = new TemplateMatcher()
}

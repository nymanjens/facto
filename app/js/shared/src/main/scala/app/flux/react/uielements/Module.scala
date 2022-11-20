package app.flux.react.uielements

import app.common.accounting.TemplateMatcher
import app.common.money.ExchangeRateManager
import app.flux.stores.InMemoryUserConfigFactory

final class Module(implicit
    templateMatcher: TemplateMatcher,
    exchangeRateManager: ExchangeRateManager,
    inMemoryUserConfigFactory: InMemoryUserConfigFactory,
) {
  implicit lazy val moneyWithCurrency: MoneyWithCurrency = new MoneyWithCurrency()
  implicit lazy val descriptionWithEntryCount: DescriptionWithEntryCount = new DescriptionWithEntryCount
}

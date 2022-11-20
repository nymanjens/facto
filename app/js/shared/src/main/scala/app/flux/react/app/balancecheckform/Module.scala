package app.flux.react.app.balancecheckform

import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.InMemoryUserConfigFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader

final class Module(implicit
    i18n: I18n,
    accountingConfig: Config,
    user: User,
    entityAccess: AppJsEntityAccess,
    exchangeRateManager: ExchangeRateManager,
    globalMessagesStore: GlobalMessagesStore,
    inMemoryUserConfigFactory: InMemoryUserConfigFactory,
    dispatcher: Dispatcher,
    clock: Clock,
    pageHeader: PageHeader,
) {

  implicit lazy val balanceCheckForm: BalanceCheckForm = new BalanceCheckForm
}

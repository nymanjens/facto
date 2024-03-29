package app.flux.react.app.balancecheckform

import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.InMemoryUserConfigStore
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
    currencyValueManager: CurrencyValueManager,
    globalMessagesStore: GlobalMessagesStore,
    inMemoryUserConfigStore: InMemoryUserConfigStore,
    dispatcher: Dispatcher,
    clock: Clock,
    pageHeader: PageHeader,
) {

  implicit lazy val balanceCheckForm: BalanceCheckForm = new BalanceCheckForm
}

package app.flux.react.app.balancecheckform

import common.I18n
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import app.flux.stores.GlobalMessagesStore
import hydro.flux.react.uielements.PageHeader
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: AppJsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   globalMessagesStore: GlobalMessagesStore,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   pageHeader: PageHeader,
) {

  implicit lazy val balanceCheckForm: BalanceCheckForm = new BalanceCheckForm
}

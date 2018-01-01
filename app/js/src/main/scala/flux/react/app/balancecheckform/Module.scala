package flux.react.app.balancecheckform

import common.I18n
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.GlobalMessagesStore
import models.access.JsEntityAccess
import models.accounting.config.Config
import models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: JsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   globalMessagesStore: GlobalMessagesStore,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit lazy val balanceCheckForm: BalanceCheckForm = wire[BalanceCheckForm]
}

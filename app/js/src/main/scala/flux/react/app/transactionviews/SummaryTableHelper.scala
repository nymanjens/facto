package flux.react.app.transactionviews

import common.I18n
import common.time.Clock
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.entries.{SummaryForYearStoreFactory, SummaryYearsStoreFactory}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.config.{Account, Config}
import models.accounting.money.ExchangeRateManager
import models.{EntityAccess, User}

private[transactionviews] final class SummaryTableHelper(account: Account)(
    implicit summaryForYearStoreFactory: SummaryForYearStoreFactory,
    summaryYearsStoreFactory: SummaryYearsStoreFactory,
    entityAccess: EntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n) {

  //
}

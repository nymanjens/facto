package app.flux.react.app.transactiongroupform

import common.I18n
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.entries.factories.LiquidationEntriesStoreFactory
import app.flux.stores.entries.factories.TagsStoreFactory
import hydro.flux.react.uielements.PageHeader
import app.models.access.AppJsEntityAccess
import app.models.access.JsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: AppJsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   globalMessagesStore: GlobalMessagesStore,
                   tagsStoreFactory: TagsStoreFactory,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   pageHeader: PageHeader,
) {

  implicit private lazy val transactionPanel: TransactionPanel = new TransactionPanel
  implicit private lazy val addTransactionPanel: AddTransactionPanel = new AddTransactionPanel
  implicit private lazy val totalFlowRestrictionInput: TotalFlowRestrictionInput =
    new TotalFlowRestrictionInput
  implicit private lazy val totalFlowInput: TotalFlowInput = new TotalFlowInput
  implicit lazy val transactionGroupForm: TransactionGroupForm = new TransactionGroupForm
}

package flux.react.app.transactiongroupform

import common.I18n
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.GlobalMessagesStore
import flux.stores.entries.factories.LiquidationEntriesStoreFactory
import flux.stores.entries.factories.TagsStoreFactory
import models.access.JsEntityAccess
import models.accounting.config.Config
import models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: JsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   globalMessagesStore: GlobalMessagesStore,
                   tagsStoreFactory: TagsStoreFactory,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory) {

  implicit private lazy val transactionPanel: TransactionPanel = new TransactionPanel
  implicit private lazy val addTransactionPanel: AddTransactionPanel = new AddTransactionPanel
  implicit private lazy val totalFlowRestrictionInput: TotalFlowRestrictionInput =
    new TotalFlowRestrictionInput
  implicit private lazy val totalFlowInput: TotalFlowInput = new TotalFlowInput
  implicit lazy val transactionGroupForm: TransactionGroupForm = new TransactionGroupForm
}

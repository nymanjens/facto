package app.flux.react.app.transactiongroupform

import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.stores.GlobalMessagesStore
import app.flux.stores.InMemoryUserConfigStore
import app.flux.stores.entries.factories.LiquidationEntriesStoreFactory
import app.flux.stores.entries.factories.TagsStoreFactory
import app.flux.stores.AttachmentStore
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
    tagsStoreFactory: TagsStoreFactory,
    dispatcher: Dispatcher,
    clock: Clock,
    liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
    pageHeader: PageHeader,
    attachmentStore: AttachmentStore,
) {

  implicit private lazy val transactionPanel: TransactionPanel = new TransactionPanel
  implicit private lazy val addTransactionPanel: AddTransactionPanel = new AddTransactionPanel
  implicit private lazy val totalFlowRestrictionInput: TotalFlowRestrictionInput =
    new TotalFlowRestrictionInput
  implicit private lazy val totalFlowInput: TotalFlowInput = new TotalFlowInput
  implicit lazy val transactionGroupForm: TransactionGroupForm = new TransactionGroupForm
}

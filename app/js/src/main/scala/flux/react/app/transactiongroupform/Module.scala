package flux.react.app.transactiongroupform

import common.I18n
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.GlobalMessagesStore
import flux.stores.entries.TagsStoreFactory
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.accounting.TransactionGroup
import models.accounting.config.Config
import models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   globalMessagesStore: GlobalMessagesStore,
                   tagsStoreFactory: TagsStoreFactory,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit private lazy val transactionPanel: TransactionPanel = wire[TransactionPanel]
  implicit private lazy val addTransactionPanel: AddTransactionPanel = wire[AddTransactionPanel]
  implicit private lazy val totalFlowRestrictionInput: TotalFlowRestrictionInput =
    wire[TotalFlowRestrictionInput]
  implicit private lazy val totalFlowInput: TotalFlowInput = wire[TotalFlowInput]
  implicit lazy val transactionGroupForm: TransactionGroupForm = wire[TransactionGroupForm]
}

package flux.react.app

import api.ScalaJsApi.GetInitialDataResponse
import common.I18n
import common.testing.TestObjects
import common.time.Clock
import flux.action.Dispatcher
import flux.react.app.transactiongroupform.TransactionGroupForm
import flux.react.app.transactionviews.Everything
import flux.stores.GlobalMessagesStore
import flux.stores.entries.{AllEntriesStoreFactory, CashFlowEntriesStoreFactory, EndowmentEntriesStoreFactory, LiquidationEntriesStoreFactory}
import models.{EntityAccess, User}
import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.config.Config
import models.accounting.money._

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   remoteDatabaseProxy: RemoteDatabaseProxy,
                   entityAccess: EntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   transactionGroupManager: TransactionGroup.Manager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  import com.softwaremill.macwire._

  implicit private lazy val menu: Menu = wire[Menu]
  implicit private lazy val globalMessages: GlobalMessages = wire[GlobalMessages]
  implicit lazy val layout: Layout = wire[Layout]

  // Configuration of submodules
  private val transactionGroupFormModule = new flux.react.app.transactiongroupform.Module
  private val transactionViewsModule = new flux.react.app.transactionviews.Module

  implicit lazy val transactionGroupForm: TransactionGroupForm =
    transactionGroupFormModule.transactionGroupForm
  implicit lazy val everything = transactionViewsModule.everything
  implicit lazy val cashFlow = transactionViewsModule.cashFlow
  implicit lazy val liquidation = transactionViewsModule.liquidation
  implicit lazy val endowments = transactionViewsModule.endowments
}

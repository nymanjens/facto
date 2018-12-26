package hydro.flux.react.uielements

import common.I18n
import hydro.common.time.Clock
import app.flux.react.uielements
import app.flux.stores._
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import app.models.access.JsEntityAccess
import app.models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   globalMessagesStore: GlobalMessagesStore,
                   pageLoadingStateStore: PageLoadingStateStore,
                   pendingModificationsStore: PendingModificationsStore,
                   applicationIsOnlineStore: ApplicationIsOnlineStore,
                   userStore: UserStore,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  lazy val pageHeader = new PageHeader
  lazy val globalMessages: GlobalMessages = new GlobalMessages
  lazy val pageLoadingSpinner: PageLoadingSpinner = new PageLoadingSpinner
  lazy val applicationDisconnectedIcon: ApplicationDisconnectedIcon = new ApplicationDisconnectedIcon
  lazy val pendingModificationsCounter: PendingModificationsCounter = new PendingModificationsCounter
}

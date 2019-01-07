package hydro.flux.react.uielements

import hydro.common.I18n
import app.flux.stores._
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import hydro.models.access.JsEntityAccess

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
  lazy val sbadminMenu: SbadminMenu = new SbadminMenu()
}

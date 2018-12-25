package hydro.flux.react.uielements.usermanagement

import common.I18n
import common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader
import hydro.flux.stores.UserStore
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   userStore: UserStore,
                   pageHeader: PageHeader) {

  private implicit lazy val updatePasswordForm = new UpdatePasswordForm
  private implicit lazy val addUserForm = new AddUserForm
  private implicit lazy val allUsersList = new AllUsersList

  lazy val userProfile: UserProfile = new UserProfile
  lazy val userAdministration: UserAdministration = new UserAdministration
}

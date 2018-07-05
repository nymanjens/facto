package flux.react.app.usermanagement

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.UserStore
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   userStore: UserStore) {

  import com.softwaremill.macwire._

  private implicit lazy val updatePasswordForm = wire[UpdatePasswordForm]
  private implicit lazy val addUserForm = wire[AddUserForm]
  private implicit lazy val allUsersList = wire[AllUsersList]

  implicit lazy val userProfile: UserProfile = wire[UserProfile]
  implicit lazy val userAdministration: UserAdministration = wire[UserAdministration]
}

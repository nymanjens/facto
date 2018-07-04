package flux.react.app.usermanagement

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import models.user.User

final class Module(implicit i18n: I18n, user: User, dispatcher: Dispatcher, clock: Clock) {

  import com.softwaremill.macwire._

  implicit lazy val userProfile: UserProfile = wire[UserProfile]
  implicit lazy val userAdministration: UserAdministration = wire[UserAdministration]
}

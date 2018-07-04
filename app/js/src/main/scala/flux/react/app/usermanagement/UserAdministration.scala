package flux.react.app.usermanagement

import common.I18n
import common.LoggingUtils.logExceptions
import flux.react.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.user.User

private[app] final class UserAdministration(implicit user: User, i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) =>
      logExceptions {
        implicit val router = props.router
        <.span("Hello world")
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}

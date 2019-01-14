package hydro.flux.react.uielements.usermanagement

import hydro.common.I18n
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class UserProfile(implicit i18n: I18n, updatePasswordForm: UpdatePasswordForm, pageHeader: PageHeader) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) => {
      implicit val router = props.router
      <.span(
        pageHeader(router.currentPage),
        Bootstrap.Row(updatePasswordForm())
      )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}

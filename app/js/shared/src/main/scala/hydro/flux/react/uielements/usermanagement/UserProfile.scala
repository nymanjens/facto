package hydro.flux.react.uielements.usermanagement

import common.I18n
import app.flux.router.RouterContext
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class UserProfile(implicit i18n: I18n, updatePasswordForm: UpdatePasswordForm, pageHeader: PageHeader) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) => {
      implicit val router = props.router
      <.span(
        pageHeader(router.currentPage),
        <.div(
          ^.className := "row",
          updatePasswordForm()
        )
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

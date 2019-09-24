package hydro.flux.react.uielements.usermanagement

import hydro.common.I18n
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

final class UserAdministration(
    implicit i18n: I18n,
    pageHeader: PageHeader,
    allUsersList: AllUsersList,
    addUserForm: AddUserForm,
) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) => {
      implicit val router = props.router
      <.span(
        pageHeader(router.currentPage),
        Bootstrap.Row(allUsersList()),
        Bootstrap.Row(addUserForm()),
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

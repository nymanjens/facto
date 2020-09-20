package app.flux.react.uielements

import hydro.common.I18n
import app.flux.router.AppPages
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object TransactionGroupEditButton {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      Bootstrap
        .Button(size = Size.xs, tag = router.anchorWithHrefTo(AppPages.EditTransactionGroup(props.groupId)))(
          Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true),
          " ",
          props.i18n("app.edit"),
        )
    })
    .build

  // **************** API ****************//
  def apply(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomElement = {
    component(Props(groupId))
  }

  // **************** Private inner types ****************//
  private case class Props(groupId: Long)(implicit val router: RouterContext, val i18n: I18n)
}

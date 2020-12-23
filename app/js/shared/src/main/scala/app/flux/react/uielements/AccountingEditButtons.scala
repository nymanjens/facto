package app.flux.react.uielements

import app.flux.router.AppPages
import hydro.common.I18n
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.router.RouterContext
import japgolly.scalajs.react.vdom.html_<^._

object AccountingEditButtons {


  def transactionGroupEditButton(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomElement = {
    Bootstrap
      .Button(size = Size.xs, tag = router.anchorWithHrefTo(AppPages.EditTransactionGroup(groupId)))(
        Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true),
        " ",
        i18n("app.edit"),
      )
  }
}

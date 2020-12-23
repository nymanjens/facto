package app.flux.react.uielements

import app.flux.router.AppPages
import hydro.common.I18n
import hydro.common.JsLoggingUtils
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react.vdom.html_<^._

object AccountingEditButtons {

  def transactionGroupEditButtons(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomArray = {
    <<.joinWithSpaces(
      Seq(
        transactionGroupEditButton(groupId),
        transactionGroupCopyButton(groupId),
      )
    )
  }

  def transactionGroupEditButton(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomElement = {
    Bootstrap
      .Button(
        Variant.default,
        Size.xs,
        tag = router.anchorWithHrefTo(AppPages.EditTransactionGroup(groupId)),
      )(
        Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true),
        " ",
        i18n("app.edit"),
      )
  }

  private def transactionGroupCopyButton(
      groupId: Long
  )(implicit router: RouterContext, i18n: I18n): VdomElement = {
    Bootstrap.Button(Variant.info, Size.xs, tag = <.a)(
      Bootstrap.FontAwesomeIcon("copy"),
      ^.onClick --> LogExceptionsCallback {
        router.setPage(AppPages.NewTransactionGroupFromCopy(transactionGroupId = groupId))
      },
    )
  }

}

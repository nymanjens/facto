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

object TransactionGroupEditButtons {

  def apply(groupId: Long)(implicit router: RouterContext, i18n: I18n): VdomArray = {
    <<.joinWithSpaces(
      Seq(
        editButton(groupId).apply(^.key := "edit"),
        copyButton(groupId).apply(^.key := "copy"),
      )
    )
  }

  private def editButton(
      groupId: Long
  )(implicit router: RouterContext, i18n: I18n): VdomTag = {
    Bootstrap
      .Button(
        Variant.default,
        Size.xs,
        tag = router.anchorWithHrefTo(AppPages.EditTransactionGroup(groupId)),
      )(
        Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true)
      )
  }

  private def copyButton(
      groupId: Long
  )(implicit router: RouterContext, i18n: I18n): VdomTag = {
    Bootstrap.Button(
      Variant.default,
      Size.xs,
      tag = router.anchorWithHrefTo(AppPages.NewTransactionGroupFromCopy(transactionGroupId = groupId)),
    )(
      Bootstrap.FontAwesomeIcon("copy")
    )
  }

}

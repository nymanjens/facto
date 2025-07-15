package app.flux.react.uielements

import app.common.accounting.TemplateMatcher
import app.common.AttachmentFormatting
import app.flux.router.AppPages
import app.flux.stores.entries.GroupedTransactions
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.BootstrapTags
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class DescriptionWithEntryCount(implicit
    templateMatcher: TemplateMatcher
) {
  private case class Props(entry: GroupedTransactions, router: RouterContext)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry

      val maybeTemplateIcon: Option[VdomNode] =
        templateMatcher.getMatchingTemplate(entry.transactions) map { template =>
          <.i(^.key := "template-icon", ^.className := template.iconClass)
        }
      val tagIndications: Seq[VdomNode] =
        entry.tags
          .map(tag =>
            Bootstrap.Label(
              BootstrapTags.toStableVariant(tag),
              tag = props.router.anchorWithHrefTo(StandardPages.Search.fromInput(s"tag:$tag")),
            )(
              ^.key := tag,
              tag,
            ): VdomNode
          )
      val attachments = entry.transactions
        .flatMap(_.attachments)
        .distinct
        .map(attachment =>
          <.a(
            ^.key := attachment.hashCode().toString,
            ^.href := AttachmentFormatting.getUrl(attachment),
            ^.target := "_blank",
            ^.className := "attachment-link",
            Bootstrap.Glyphicon("paperclip"),
          )
        )
      val centralContent =
        <<.joinWithSpaces(
          maybeTemplateIcon.toVector ++ tagIndications :+ attachments.toVdomArray :+ (entry.description: VdomNode)
        )

      if (entry.transactions.size == 1) {
        centralContent
      } else {
        UpperRightCorner(cornerContent = s"(${entry.transactions.size})")(centralContent)
      }
    })
    .build

  def apply(entry: GroupedTransactions)(implicit router: RouterContext): VdomElement = {
    component(Props(entry, router))
  }
}

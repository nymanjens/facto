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
  private case class State(showDetailDescriptions: Boolean = false)

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State())
    .renderPS(($, props, state) => {
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
              tag = props.router.anchorWithHrefTo(StandardPages.Search.fromInput(s"tagExact:$tag")),
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

      val hasDetailDescriptions = entry.transactions.exists(_.detailDescription.nonEmpty)
      val detailDescriptions = entry.transactions.flatMap(_.detailDescription.split("\n")).filter(_.nonEmpty)

      val detailContent = if (state.showDetailDescriptions && hasDetailDescriptions) {
        <.div(^.className := "detail-descriptions")(
          detailDescriptions.map(desc => <.div(^.key := desc, desc)).toVdomArray
        )
      } else {
        EmptyVdom
      }

      val cornerContent: Seq[VdomNode] = {
        val countText: Option[VdomNode] =
          if (entry.transactions.size == 1) None else Some(s"(${entry.transactions.size})")
        val detailButton: Option[VdomNode] = if (hasDetailDescriptions) {
          Some(
            Bootstrap.Button(variant = Bootstrap.Variant.default, size = Bootstrap.Size.xs)(
              ^.key := "detail-toggle",
              ^.onClick --> $.modState(state =>
                state.copy(showDetailDescriptions = !state.showDetailDescriptions)
              ),
              Bootstrap.Glyphicon("align-left"),
            )
          )
        } else {
          None
        }
        countText.toSeq ++ detailButton.toSeq
      }

      val mainContent = if (entry.transactions.size == 1 && !hasDetailDescriptions) {
        centralContent
      } else {
        UpperRightCorner(cornerContent = cornerContent: _*)(centralContent)
      }

      <.div(^.className := "description-with-entry-count")(
        mainContent,
        detailContent,
      )
    })
    .build

  def apply(entry: GroupedTransactions)(implicit router: RouterContext): VdomElement = {
    component(Props(entry, router))
  }
}
